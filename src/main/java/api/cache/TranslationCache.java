package api.cache;

public interface TranslationCache {
    String get(String input, String from, String to);

    void put(String input, String from, String to, String output);

    default void close(){
        // no-op by default
    }
}
