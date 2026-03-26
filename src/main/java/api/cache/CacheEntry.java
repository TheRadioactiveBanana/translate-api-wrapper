package api.cache;

import org.bson.codecs.pojo.annotations.BsonCreator;

import java.util.Objects;

public class CacheEntry {
    public String input;
    public String from;
    public String to;
    public String output;

    @BsonCreator
    @SuppressWarnings("unused")
    public CacheEntry(){}

    public CacheEntry(String input, String from, String to, String output){
        this.input = input;
        this.from = from;
        this.to = to;
        this.output = output;
    }

    public CacheEntry(String input, String from, String to){
        this.input = input;
        this.from = from;
        this.to = to;
        this.output = null;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof CacheEntry that)) return false;
        return Objects.equals(input, that.input)
            && Objects.equals(from, that.from)
            && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode(){
        return Objects.hash(input, from, to);
    }
}
