package api.clients;

import api.language.Language;

public interface TranslationClient {


    String translate(String text, Language from, Language to);
}
