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
}
