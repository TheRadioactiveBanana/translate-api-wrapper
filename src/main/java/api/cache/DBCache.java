package api.cache;

import com.github.benmanes.caffeine.cache.*;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.annotations.BsonCreator;

import java.util.concurrent.TimeUnit;

import static org.bson.codecs.configuration.CodecRegistries.*;

public class DBCache implements TranslationCache {

    private final Cache<CacheKey, String> local;
    private final MongoClient client;
    private final MongoCollection<CacheEntry> collection;

    public DBCache(String uri, String database){
        if(uri == null || uri.isBlank()) throw new IllegalArgumentException("URI must not be null or blank");

        if(database == null || database.isBlank()) throw new IllegalArgumentException("Database must not be null or blank");


        this.local = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

        this.client = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build()
        );

        CodecRegistry registry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        this.collection = client.getDatabase(database)
            .withCodecRegistry(registry)
            .getCollection("translation", CacheEntry.class);
    }

    public String get(String input, String from, String to){
        CacheKey key = new CacheKey(input, from, to);

        String value = local.getIfPresent(key);

        if(value != null) return value;

        CacheEntry entry = collection.find(Filters.and(
            Filters.eq("input", input),
            Filters.eq("from", from),
            Filters.eq("to", to)
        )).first();

        if(entry == null) return null;
        local.put(key, entry.output);
        return entry.output;
    }

    public void put(String input, String from, String to, String output){
        CacheKey key = new CacheKey(input, from, to);
        local.put(key, output);

        CacheEntry entry = new CacheEntry(input, from, to, output);
        collection.replaceOne(
            Filters.and(
                Filters.eq("input", input),
                Filters.eq("from", from),
                Filters.eq("to", to)
            ),
            entry,
            new ReplaceOptions().upsert(true)
        );
    }

    public void close(){
        client.close();
    }

    private record CacheKey(String input, String from, String to) {

    }

    public static class CacheEntry {
        public String input;
        public String from;
        public String to;
        public String output;

        @BsonCreator
        @SuppressWarnings("unused")
        public CacheEntry(){
        }

        public CacheEntry(String input, String from, String to, String output){
            this.input = input;
            this.from = from;
            this.to = to;
            this.output = output;
        }
    }
}
