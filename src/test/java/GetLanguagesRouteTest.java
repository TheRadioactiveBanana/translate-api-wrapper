import api.clients.TranslationClient;
import api.language.Language;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetLanguagesRouteTest extends ApiTestSupport {

    @Test
    void returnsSupportedLanguagesAsJson() throws Exception {
        TranslationClient translator = new TranslationClient() {
            @Override
            public String backend() {
                return "stub";
            }

            @Override
            public String translate(String text, Language from, Language to) {
                return text;
            }
        };

        try(var server = startServer(translator);
            var response = execute(request(server, "/api/languages").get().build())){

            assertEquals(200, response.code());

            JSONArray languages = JSON.parseArray(response.body().string());
            assertTrue(languages.size() > 10);

            JSONObject english = languages.stream()
                .map(JSONObject.class::cast)
                .filter(language -> "en".equals(language.getString("code")))
                .findFirst()
                .orElseThrow();

            assertEquals("English", english.getString("name"));
        }
    }
}
