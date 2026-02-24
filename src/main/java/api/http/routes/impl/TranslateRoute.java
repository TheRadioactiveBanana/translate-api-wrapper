package api.http.routes.impl;

import api.cache.TranslationCache;
import api.clients.*;
import api.http.routes.AbstractHTTPRoute;
import api.language.Language;
import io.javalin.http.*;
import org.slf4j.*;

import static api.TranslateAPIMain.*;

public class TranslateRoute extends AbstractHTTPRoute {

    private static final Logger log = LoggerFactory.getLogger(TranslateRoute.class);
    private final int maxTextLength;
    private final TranslationCache cache;

    public TranslateRoute(int maxTextLength, TranslationCache cache){
        this.maxTextLength = maxTextLength;
        this.cache = cache;
    }

    private static TranslationClient resolveClient(String backend){
        return switch(backend){
            case "google" -> translators.stream().filter(t->t instanceof GoogleTranslator).findFirst().orElse(null);
            case "deepl" -> translators.stream().filter(t->t instanceof DeepLTranslator).findFirst().orElse(null);
            case "libre" -> throw new NotImplementedResponse("LibreTranslate currently not implemented");
            case null -> translators.stream().findAny().orElse(null);
            default -> null;
        };
    }

    @Override
    public String execute(Context ctx){
        String requestToken = ctx.header("token");
        if(requestToken == null) throw new BadRequestResponse("Missing token");
        if(!requestToken.equals(token)) throw new ForbiddenResponse("Invalid token");

        var text = ctx.body();

        if(text.isBlank()) throw new BadRequestResponse("Missing text");
        if(maxTextLength > 0 && text.length() > maxTextLength) throw new ContentTooLargeResponse("Text too long");

        var to = Language.fromCode(ctx.header("to"));

        if(to == null) throw new BadRequestResponse("Missing language to.");

        var from = Language.fromCode(ctx.header("from"));

        if(from == null) from = Language.auto;

        var client = resolveClient(ctx.header("backend"));

        if(client == null) throw new NotFoundResponse("Backend not found");

        try{
            long time = System.currentTimeMillis();

            String cached = cache.get(text, from.code, to.code);

            if(cached != null){
                ctx.header("backend", "cache");
                ctx.header("time", String.valueOf(System.currentTimeMillis() - time));
                return cached;
            }

            var result = client.translate(text, from, to);

            if(result == null || result.isBlank()){
                throw new IllegalStateException("Translation backend returned empty response");
            }

            cache.put(text, from.code, to.code, result);
            ctx.header("backend", client.getClass().getSimpleName());
            ctx.header("time", String.valueOf(System.currentTimeMillis() - time));
            return result;
        }catch(Exception e){
            log.error("Error in translator of type {}", e.getClass().getSimpleName(), e);
            throw new InternalServerErrorResponse("Failed to translate.");
        }
    }
}
