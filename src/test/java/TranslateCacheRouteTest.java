import api.clients.TranslationClient;
import api.language.Language;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslateCacheRouteTest extends ApiTestSupport {

    @Test
    void servesRepeatedTranslationsFromCache() throws Exception {
        var calls = new AtomicInteger();

        TranslationClient translator = new TranslationClient() {
            @Override
            public String backend() {
                return "stub";
            }

            @Override
            public String translate(String text, Language from, Language to) {
                calls.incrementAndGet();
                return "bonjour";
            }
        };

        try(var server = startServer(translator);
            var first = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .header("to", "fr")
                .post(textBody("hello"))
                .build());
            var second = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .header("to", "fr")
                .post(textBody("hello"))
                .build())){

            assertEquals(200, first.code());
            assertEquals("stub", first.header("backend"));
            assertEquals("bonjour", first.body().string());

            assertEquals(200, second.code());
            assertEquals("cache", second.header("backend"));
            assertEquals("bonjour", second.body().string());
            assertEquals(1, calls.get());
        }
    }
}
