package api;

import api.cache.*;
import api.clients.*;
import api.config.Config;
import api.http.WebServer;
import org.slf4j.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TranslateAPIMain {

    private static final Logger log = LoggerFactory.getLogger(TranslateAPIMain.class);

    public static String token;
    public static WebServer webServer;
    public static TranslationCache cache;

    public static Set<TranslationClient> translators = ConcurrentHashMap.newKeySet();

    static void main(String[] args){
        Config.init();

        if(args.length > 0) token = args[0];
        else token = resolveToken();

        if(token == null) throw new IllegalArgumentException("No token found.");

        setupClients();

        cache = setupCache();
        webServer = new WebServer(Config.string("host"), Config.integer("port"));

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(TranslateAPIMain::stop));
    }

    private static void setupClients(){
        if(Config.bool("google")) translators.add(new GoogleTranslator());
        if(Config.bool("deepl")) translators.add(new DeepLTranslator(Config.string("deepl-auth-key")));
        if(Config.bool("libre")) translators.add(new LibreTranslator(Config.string("libre-url")));

        if(translators.isEmpty()) throw new IllegalArgumentException("No translators configured!");
    }

    private static TranslationCache setupCache(){
        int maxEntries = Config.integerOr("cache.max-entries", 1000);
        int expireMinutes = Config.integerOr("cache.expire-minutes", 10);
        String type = Config.stringOr("cache.type", "memory").trim().toLowerCase();

        return switch(type){
            case "memory", "in-memory", "inmemory" -> new InMemoryCache(maxEntries, expireMinutes);
            case "db", "database", "mongodb", "mongo" ->
                new DBCache(Config.string("db.uri"), Config.string("db.database"), maxEntries, expireMinutes);
            default -> throw new IllegalArgumentException("Unsupported cache.type: " + type);
        };
    }

    private static void stop(){
        log.info("Exiting.");

        if(cache != null) cache.close();
        webServer.stop();
    }

    private static String resolveToken(){
        return Config.string("token");
    }
}
