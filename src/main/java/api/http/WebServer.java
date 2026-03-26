package api.http;

import api.config.Config;
import api.http.filters.impl.*;
import api.http.routes.impl.*;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;
import io.javalin.http.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.*;

import java.util.function.Consumer;

import static api.TranslateAPIMain.cache;
import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer implements Consumer<JavalinConfig>, EndpointGroup, RequestLogger {

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    public final String host;
    public final int port;
    private final Javalin javalin;

    public WebServer(String host, int port){
        this.host = host;
        this.port = port;

        javalin = Javalin.create(this).start();
    }

    @Override
    public void accept(JavalinConfig config){
        config.jetty.host = host;
        config.jetty.port = port;
        config.routes.apiBuilder(this);
        config.requestLogger.http(this);

        if(Config.boolOr("proxy", true)){
            config.contextResolver.ip = WebServer::resolveClientIp;
        }
    }

    @Override
    public void addEndpoints(){
        before(new RateLimitFilter(
            Config.integerOr("rate-limit.spacing-ms", 1000),
            Config.integerOr("rate-limit.cap", 5),
            Config.integerOr("rate-limit.max-entries", 1000),
            Config.integerOr("rate-limit.expire-minutes", 1)
        ));

        path("api", ()->{
            before("translate", new AuthFilter());
            post("translate", new TranslateRoute(Config.integer("max-size"), cache));

            get("languages", new GetLanguagesRoute());
        });
    }

    @Override
    public void handle(@NotNull Context ctx, @NotNull Float executionTimeMs){
        log.info(
            "[{}ms] {} - {} -> {}",
            executionTimeMs,
            ctx.path(),
            ctx.status().getCode() + ctx.status().getMessage(),
            ctx.ip()
        );
    }

    public void stop(){
        javalin.stop();
    }

    private static String resolveClientIp(Context ctx){
        String forwarded = ctx.header(Header.FORWARDED);
        if(forwarded != null){
            for(String part : forwarded.split(",")){
                for(String token : part.split(";")){
                    String trimmed = token.trim();
                    if(trimmed.regionMatches(true, 0, "for=", 0, 4)){
                        String value = trimmed.substring(4).trim();
                        if(value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2){
                            value = value.substring(1, value.length() - 1);
                        }
                        if(value.startsWith("[")){
                            int end = value.indexOf(']');
                            if(end > 0) return value.substring(1, end);
                        }
                        int lastColon = value.lastIndexOf(':');
                        if(lastColon > 0 && value.indexOf(':') == lastColon){
                            return value.substring(0, lastColon);
                        }
                        return value;
                    }
                }
            }
        }

        String xForwardedFor = ctx.header(Header.X_FORWARDED_FOR);
        if(xForwardedFor != null){
            String first = xForwardedFor.split(",")[0].trim();
            if(!first.isEmpty()) return first;
        }

        String realIp = ctx.header("X-Real-IP");
        if(realIp != null && !realIp.isBlank()){
            return realIp.trim();
        }

        return ctx.req().getRemoteAddr();
    }
}
