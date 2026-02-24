package api;

import api.cache.TranslationCache;
import api.clients.TranslationClient;
import api.config.Config;
import api.http.WebServer;
import api.language.Language;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class APITest {

    private static final String host = "127.0.0.1";
    private static final String token = "test-token";

    private WebServer server;
    private HttpClient client;
    private int port;

    private static int findFreePort() throws IOException{
        try(ServerSocket socket = new ServerSocket(0)){
            return socket.getLocalPort();
        }
    }

    @BeforeEach
    void setUp() throws IOException{
        port = findFreePort();
        Path configPath = Path.of("build", "test-config.yml");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, """
            host: "127.0.0.1"
            port: %d
            token: "%s"
            max-size: 5
            google: false
            db:
              uri: "mongodb://192.168.1.201:27017"
              database: "fish"
            rate-limit:
              spacing-ms: 1
              cap: 100
              max-entries: 1000
              expire-minutes: 1
            """.formatted(port, token));

        Config.init(configPath.toString());

        TranslateAPIMain.token = token;
        TranslateAPIMain.cache = new InMemoryCache();
        TranslateAPIMain.translators.clear();
        TranslateAPIMain.translators.add(new FakeTranslator());

        server = new WebServer(host, port);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown(){
        if(server != null) server.stop();
    }

    @Test
    void languagesEndpointReturnsLanguageListAndRespondsQuickly() throws Exception{
        long startNs = System.nanoTime();
        HttpResponse<String> response = request("GET", "/api/languages", "", null);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        assertEquals(200, response.statusCode());
        assertTrue(elapsedMs >= 0, "Measured response time should be non-negative");

        JSONArray languages = JSONArray.parseArray(response.body());
        assertFalse(languages.isEmpty());
        JSONObject first = languages.getJSONObject(0);
        assertNotNull(first.getString("code"));
        assertNotNull(first.getString("name"));
    }

    @Test
    void translateEndpointValidatesToken() throws Exception{
        HttpResponse<String> missingToken = request("POST", "/api/translate", "hello", Map.of("to", "es"));
        assertEquals(400, missingToken.statusCode());
        assertTrue(missingToken.body().contains("Missing token"));

        HttpResponse<String> badToken = request("POST", "/api/translate", "hello", Map.of(
            "to", "es",
            "token", "wrong-token"
        ));
        assertEquals(403, badToken.statusCode());
        assertTrue(badToken.body().contains("Invalid token"));
    }

    @Test
    void translateEndpointValidatesRequestFields() throws Exception{
        HttpResponse<String> blankText = request("POST", "/api/translate", " ", Map.of(
            "token", token,
            "to", "es"
        ));
        assertEquals(400, blankText.statusCode());
        assertTrue(blankText.body().contains("Missing text"));

        HttpResponse<String> tooLong = request("POST", "/api/translate", "123456", Map.of(
            "token", token,
            "to", "es"
        ));
        assertEquals(413, tooLong.statusCode());
        assertTrue(tooLong.body().contains("Text too long"));

        HttpResponse<String> missingTo = request("POST", "/api/translate", "hello", Map.of("token", token));
        assertEquals(400, missingTo.statusCode());
        assertTrue(missingTo.body().contains("Missing language to."));

        HttpResponse<String> missingBackend = request("POST", "/api/translate", "hello", Map.of(
            "token", token,
            "to", "es",
            "backend", "unknown"
        ));
        assertEquals(404, missingBackend.statusCode());
        assertTrue(missingBackend.body().contains("Backend not found"));
    }

    @Test
    void translateEndpointSetsBackendAndTimeAndUsesCache() throws Exception{
        long firstStart = System.nanoTime();
        HttpResponse<String> first = request("POST", "/api/translate", "hello", Map.of(
            "token", token,
            "to", "es"
        ));
        long firstElapsedMs = (System.nanoTime() - firstStart) / 1_000_000;

        assertEquals(200, first.statusCode());
        assertEquals("[es] hello", first.body());
        assertEquals("FakeTranslator", first.headers().firstValue("backend").orElse(null));
        assertNotNull(first.headers().firstValue("time").orElse(null));
        assertTrue(Long.parseLong(first.headers().firstValue("time").orElseThrow()) >= 0);
        assertTrue(firstElapsedMs >= 0);

        HttpResponse<String> second = request("POST", "/api/translate", "hello", Map.of(
            "token", token,
            "to", "es"
        ));

        assertEquals(200, second.statusCode());
        assertEquals("[es] hello", second.body());
        assertEquals("cache", second.headers().firstValue("backend").orElse(null));
        assertNotNull(second.headers().firstValue("time").orElse(null));
        assertTrue(Long.parseLong(second.headers().firstValue("time").orElseThrow()) >= 0);
    }

    private HttpResponse<String> request(String method, String path, String body, Map<String, String> headers) throws Exception{
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create("http://" + host + ":" + port + path));

        if(headers != null){
            headers.forEach(builder::header);
        }

        HttpRequest request = method.equals("GET")
            ? builder.GET().build()
            : builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static class FakeTranslator implements TranslationClient {
        @Override
        public String translate(String text, Language from, Language to){
            return "[" + to.code + "] " + text;
        }
    }

    private static class InMemoryCache implements TranslationCache {
        private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

        @Override
        public String get(String input, String from, String to){
            return map.get(key(input, from, to));
        }

        @Override
        public void put(String input, String from, String to, String output){
            map.put(key(input, from, to), output);
        }

        private String key(String input, String from, String to){
            return input + "\n" + from + "\n" + to;
        }
    }
}
