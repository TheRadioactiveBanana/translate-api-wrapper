package api.cache;

import com.github.benmanes.caffeine.cache.*;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.*;
import static org.bson.codecs.configuration.CodecRegistries.*;

public class DBCache implements TranslationCache {

    private final Cache<CacheEntry, String> local;
    private final MongoClient client;
    private final MongoCollection<CacheEntry> collection;

    public DBCache(String uri, String database, int maxEntries, int expireMinutes){
        if(uri == null || uri.isBlank()) throw new IllegalArgumentException("URI must not be null or blank");
        if(database == null || database.isBlank()) database = "fish";
        if(maxEntries <= 0) maxEntries = 0;
        if(expireMinutes <= 0) expireMinutes = 0;

        this.local = Caffeine.newBuilder()
            .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
            .maximumSize(maxEntries)
            .build();

        this.client = MongoClients.create(uri);

        CodecRegistry registry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        this.collection = client.getDatabase(database)
            .withCodecRegistry(registry)
            .getCollection("translation", CacheEntry.class);
    }

    public String get(String input, String from, String to){
        CacheEntry key = new CacheEntry(input, from, to);

        String value = local.getIfPresent(key);

        if(value != null) return value;

        CacheEntry entry = collection.find(and(
            eq("input", input),
            eq("from", from),
            eq("to", to)
        )).first();

        if(entry == null) return null;
        local.put(key, entry.output);
        return entry.output;
    }

    public void put(String input, String from, String to, String output){
        CacheEntry key = new CacheEntry(input, from, to);
        local.put(key, output);

        CacheEntry entry = new CacheEntry(input, from, to, output);
        collection.replaceOne(
            and(
                eq("input", input),
                eq("from", from),
                eq("to", to)
            ),
            entry,
            new ReplaceOptions().upsert(true)
        );
    }

    public void close(){
        client.close();
    }
}
