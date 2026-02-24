package api.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.util.List;

@SuppressWarnings("unused")
public final class Config {

    private static final String defaultPath = "config.yml";
    private static ConfigurationNode root;

    public static void init(){
        init(defaultPath);
    }

    public static void init(String fileName){
        File file = new File(fileName);
        if(!file.isFile()){
            throw new IllegalStateException("YAML config missing or not a file: " + file.getAbsolutePath());
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(file.toPath())
            .build();
        try{
            root = loader.load();
        }catch(IOException e){
            throw new IllegalStateException("Failed to load YAML config: " + file.getAbsolutePath(), e);
        }
    }

    private static ConfigurationNode node(String path){
        return root == null ? null : root.node((Object[]) path.split("\\."));
    }

    private static <T> T get(String path, Class<T> type){
        ConfigurationNode n = node(path);
        if(n == null || n.virtual()) return null;
        try{
            return n.get(type);
        }catch(SerializationException e){
            return null;
        }
    }

    private static <T> T getOr(String path, Class<T> type, T fallback){
        T value = get(path, type);
        return value == null ? fallback : value;
    }

    public static Object object(String key){
        return get(key, Object.class);
    }

    public static Object objectOr(String key, Object fallback){
        return getOr(key, Object.class, fallback);
    }

    public static int integer(String key){
        Integer v = get(key, Integer.class);
        return v == null ? 0 : v;
    }

    public static int integerOr(String key, int fallback){
        return getOr(key, Integer.class, fallback);
    }

    public static boolean bool(String key){
        Boolean v = get(key, Boolean.class);
        return v != null && v;
    }

    public static boolean boolOr(String key, boolean fallback){
        return getOr(key, Boolean.class, fallback);
    }

    public static String string(String key){
        return get(key, String.class);
    }

    public static String stringOr(String key, String fallback){
        return getOr(key, String.class, fallback);
    }

    public static <T> List<T> list(String key, Class<T> type){
        ConfigurationNode n = node(key);
        if(n == null || n.virtual()) return List.of();
        try{
            List<T> l = n.getList(type);
            return l == null ? List.of() : l;
        }catch(SerializationException e){
            return List.of();
        }
    }

    public static List<String> list(String key){
        return list(key, String.class);
    }

    public static <T> List<T> listOr(String key, Class<T> type, List<T> fallback){
        List<T> values = list(key, type);
        return values.isEmpty() ? fallback : values;
    }

    public static List<String> listOr(String key, List<String> fallback){
        return listOr(key, String.class, fallback);
    }
}
