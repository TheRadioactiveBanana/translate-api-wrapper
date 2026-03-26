package api.http.routes.impl;

import api.cache.TranslationCache;
import api.clients.*;
import api.http.routes.AbstractHTTPRoute;
import api.language.Language;
import io.javalin.http.*;
import org.slf4j.*;

import static api.TranslateAPIMain.*;
import static api.language.Language.auto;
import static java.lang.Integer.compare;

public class TranslateRoute extends AbstractHTTPRoute {

    private static final Logger log = LoggerFactory.getLogger(TranslateRoute.class);
    private final int maxTextLength;
    private final TranslationCache cache;

    public TranslateRoute(int maxTextLength, TranslationCache cache){
        this.maxTextLength = maxTextLength;
        this.cache = cache;
    }

    @Override
    public String execute(Context ctx){
        var text = ctx.body();

        if(text.isBlank()) throw new BadRequestResponse("Missing text");

        if(maxTextLength > 0 && text.length() > maxTextLength) throw new ContentTooLargeResponse("Text too long");

        var to = Language.fromCode(ctx.header("to"));

        if(to == null || to == auto) throw new BadRequestResponse("Missing language to.");

        var from = Language.fromCode(ctx.header("from"));

        if(from == null) from = auto;

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
            ctx.header("backend", client.backend());
            ctx.header("time", String.valueOf(System.currentTimeMillis() - time));
            return result;
        }catch(Exception e){
            log.error("Error in translator of type {}", e.getClass().getSimpleName(), e);
            throw new InternalServerErrorResponse("Failed to translate.");
        }
    }

    private static TranslationClient resolveClient(String backend){
        if(backend == null){
            return translators.stream()
                .max((a, b)->compare(b.quality(), a.quality()))
                .orElse(null);
        }
        else return translators.stream().filter(t->t.backend().equals(backend.toLowerCase())).findFirst().orElse(null);
    }
}
