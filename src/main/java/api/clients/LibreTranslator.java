package api.clients;

import api.language.Language;
import net.suuft.libretranslate.Translator;

public class LibreTranslator implements TranslationClient {

    public LibreTranslator(String url){
        //don't use the centrally hosted one
        if(url == null) throw new IllegalArgumentException("LibreTranslate URL cannot be null");

        Translator.setUrlApi(url);
    }

    @Override
    public String translate(String text, Language from, Language to){
        try{
            var source = from == Language.auto ? "auto" : normalizeCode(from.code);
            var target = normalizeCode(to.code);
            return Translator.translate(source, target, text);
        }catch(Exception e){
            throw new RuntimeException("LibreTranslate request failed", e);
        }
    }

    private static String normalizeCode(String code){
        if(code == null) return null;
        return code.toLowerCase();
    }
}
