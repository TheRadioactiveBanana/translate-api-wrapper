import api.clients.TranslationClient;
import api.language.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TranslateRouteTest extends ApiTestSupport {

    @Test
    void translatesTextWithRequestedBackend() throws Exception {
        TranslationClient translator = new TranslationClient() {
            @Override
            public String backend() {
                return "stub";
            }

            @Override
            public int quality(){
                return 0;
            }

            @Override
            public String translate(String text, Language from, Language to) {
                return "[" + from.code + "->" + to.code + "] " + text.toUpperCase();
            }
        };

        try(var server = startServer(translator);
            var response = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .header("from", "en")
                .header("to", "fr")
                .header("backend", "stub")
                .post(textBody("hello fish"))
                .build())){

            assertEquals(200, response.code());
            assertEquals("stub", response.header("backend"));
            assertNotNull(response.header("time"));
        }
    }

    @Test
    void translatesTextWithHighestQualityBackendWhenBackendIsNotSpecified() throws Exception {
        TranslationClient lowQualityTranslator = new TranslationClient() {
            @Override
            public String backend() {
                return "low";
            }

            @Override
            public int quality() {
                return 10;
            }

            @Override
            public String translate(String text, Language from, Language to) {
                return "low";
            }
        };

        TranslationClient highQualityTranslator = new TranslationClient() {
            @Override
            public String backend() {
                return "high";
            }

            @Override
            public int quality() {
                return 90;
            }

            @Override
            public String translate(String text, Language from, Language to) {
                return "high";
            }
        };

        try(var server = startServer(lowQualityTranslator, highQualityTranslator);
            var response = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .header("from", "en")
                .header("to", "fr")
                .post(textBody("hello fish"))
                .build())){

            assertEquals(200, response.code());
            assertEquals("high", response.header("backend"));
            assertEquals("high", response.body().string());
        }
    }
}
