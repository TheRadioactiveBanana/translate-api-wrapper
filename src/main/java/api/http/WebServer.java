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

        javalin = Javalin.createAndStart(this);
    }

    @Override
    public void accept(JavalinConfig config){
        config.jetty.defaultHost = host;
        config.jetty.defaultPort = port;
        config.router.apiBuilder(this);
        config.requestLogger.http(this);
    }

    @Override
    public void addEndpoints(){
        long spacingMs = Config.integerOr("rate-limit.spacing-ms", 1000);
        int cap = Config.integerOr("rate-limit.cap", 5);
        int maxEntries = Config.integerOr("rate-limit.max-entries", 1000);
        int expireMinutes = Config.integerOr("rate-limit.expire-minutes", 1);

        before(new RateLimitFilter(spacingMs, cap, maxEntries, expireMinutes));

        path("api", ()->{
            before("/translate", new AuthFilter());
            post("translate", new TranslateRoute(Config.integer("max-size"), cache));

            get("languages", new GetLanguagesRoute());
        });
    }

    @Override
    public void handle(@NotNull Context ctx, @NotNull Float executionTimeMs){
        log.info("TX -> {}/{} {} {}ms", ctx.ip(), ctx.path(), ctx.statusCode(), executionTimeMs);
    }

    public void stop(){
        javalin.stop();
    }
}
