package api.clients;

import api.language.Language;
import com.deepl.api.*;

public class DeepLTranslator implements TranslationClient {

    private final Translator client;

    public DeepLTranslator(String authKey){
        if(authKey == null || authKey.isBlank()) throw new IllegalArgumentException("DeepL auth key is missing");

        this.client = new Translator(authKey);
    }

    @Override
    public String translate(String text, Language from, Language to){
        try{
            var source = from == Language.auto ? null : normalizeCode(from.code);
            var target = normalizeCode(to.code);

            var result = client.translateText(text, source, target);

            return result.getText();
        }catch(DeepLException | InterruptedException e){
            throw new RuntimeException("DeepL translator request failed", e);
        }
    }

    private static String normalizeCode(String code){
        return switch(code.toLowerCase()){
            case "zh-cn", "zh-tw" -> "ZH";
            default -> code.toUpperCase();
        };
    }
}
