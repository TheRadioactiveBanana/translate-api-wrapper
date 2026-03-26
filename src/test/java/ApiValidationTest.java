import api.clients.TranslationClient;
import api.language.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiValidationTest extends ApiTestSupport {

    @Test
    void rejectsMissingAuthInvalidAuthAndBadTranslateInput() throws Exception {
        TranslationClient translator = new TranslationClient() {
            @Override
            public String backend() {
                return "stub";
            }

            @Override
            public String translate(String text, Language from, Language to) {
                return "unused";
            }
        };

        try(var server = startServer(translator);
            var missingToken = execute(request(server, "/api/translate")
                .header("to", "fr")
                .post(textBody("hello"))
                .build());
            var invalidToken = execute(request(server, "/api/translate")
                .header("token", "wrong-token")
                .header("to", "fr")
                .post(textBody("hello"))
                .build());
            var missingLanguage = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .post(textBody("hello"))
                .build());
            var blankBody = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .header("to", "fr")
                .post(textBody("   "))
                .build());
            var unknownBackend = execute(request(server, "/api/translate")
                .header("token", TOKEN)
                .header("to", "fr")
                .header("backend", "missing")
                .post(textBody("hello"))
                .build())){

            assertEquals(400, missingToken.code());
            assertEquals("Missing token", missingToken.body().string());

            assertEquals(403, invalidToken.code());
            assertEquals("Invalid token", invalidToken.body().string());

            assertEquals(400, missingLanguage.code());
            assertEquals("Missing language to.", missingLanguage.body().string());

            assertEquals(400, blankBody.code());
            assertEquals("Missing text", blankBody.body().string());

            assertEquals(404, unknownBackend.code());
            assertEquals("Backend not found", unknownBackend.body().string());
        }
    }
}
