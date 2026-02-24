package api.language;

import java.util.*;

public enum Language {

    english("en", "English"),
    spanish("es", "Spanish"),
    french("fr", "French"),
    german("de", "German"),
    italian("it", "Italian"),
    portuguese("pt", "Portuguese"),
    russian("ru", "Russian"),
    japanese("ja", "Japanese"),
    chineseSimplified("zh-CN", "Chinese (Simplified)"),
    chineseTraditional("zh-TW", "Chinese (Traditional)"),
    korean("ko", "Korean"),
    dutch("nl", "Dutch"),
    polish("pl", "Polish"),
    swedish("sv", "Swedish"),
    norwegian("no", "Norwegian"),
    danish("da", "Danish"),
    finnish("fi", "Finnish"),
    turkish("tr", "Turkish"),
    greek("el", "Greek"),
    czech("cs", "Czech"),
    romanian("ro", "Romanian"),
    hungarian("hu", "Hungarian"),
    ukrainian("uk", "Ukrainian"),
    thai("th", "Thai"),
    vietnamese("vi", "Vietnamese"),
    indonesian("id", "Indonesian"),
    malay("ms", "Malay"),
    hebrew("he", "Hebrew"),
    persian("fa", "Persian"),
    bengali("bn", "Bengali"),
    swahili("sw", "Swahili"),
    catalan("ca", "Catalan"),
    slovak("sk", "Slovak"),
    bulgarian("bg", "Bulgarian"),
    croatian("hr", "Croatian"),
    serbian("sr", "Serbian"),
    latvian("lv", "Latvian"),
    lithuanian("lt", "Lithuanian"),
    estonian("et", "Estonian"),
    slovenian("sl", "Slovenian"),
    albanian("sq", "Albanian"),
    macedonian("mk", "Macedonian"),
    afrikaans("af", "Afrikaans"),
    icelandic("is", "Icelandic"),
    welsh("cy", "Welsh"),
    irish("ga", "Irish"),
    basque("eu", "Basque"),
    galician("gl", "Galician"),
    latin("la", "Latin"),
    auto("auto", "Auto-Detect");

    private static final Map<String, Language> languageRegistry = new HashMap<>();

    static{
        for(Language language : values()){
            languageRegistry.put(language.code.toLowerCase(), language);
        }
        languageRegistry.put("iw", hebrew);
    }

    public final String code;
    public final String name;

    Language(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static Language fromCode(String code){
        if(code == null) return null;

        return languageRegistry.get(code.toLowerCase());
    }
}
