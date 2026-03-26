import api.TranslateAPIMain;
import api.cache.InMemoryCache;
import api.clients.TranslationClient;
import api.config.Config;
import api.http.WebServer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;

abstract class ApiTestSupport {

    static final String TOKEN = "example-token";
    static final OkHttpClient CLIENT = new OkHttpClient();
    static final MediaType TEXT = MediaType.get("text/plain; charset=utf-8");

    static{
        Config.init("config-test.yml");
    }

    static TestServer startServer(TranslationClient... translators) {
        var port = findFreePort();

        TranslateAPIMain.token = TOKEN;
        TranslateAPIMain.cache = new InMemoryCache(100, 10);
        TranslateAPIMain.translators.clear();
        TranslateAPIMain.translators.addAll(Set.of(translators));

        return new TestServer(new WebServer("127.0.0.1", port), port);
    }

    static Request.Builder request(TestServer server, String path) {
        return new Request.Builder().url(server.url(path));
    }

    static RequestBody textBody(String value) {
        return RequestBody.create(value, TEXT);
    }

    static Response execute(Request request) throws IOException {
        return CLIENT.newCall(request).execute();
    }

    private static int findFreePort() {
        try(var socket = new ServerSocket(0)){
            return socket.getLocalPort();
        }catch(IOException e){
            throw new IllegalStateException("Failed to reserve test port", e);
        }
    }

    static final class TestServer implements AutoCloseable {
        private final WebServer server;
        private final int port;

        TestServer(WebServer server, int port) {
            this.server = server;
            this.port = port;
        }

        String url(String path) {
            return "http://127.0.0.1:" + port + path;
        }

        @Override
        public void close() {
            server.stop();
            if(TranslateAPIMain.cache != null){
                TranslateAPIMain.cache.close();
            }
            TranslateAPIMain.translators.clear();
        }
    }
}
