package api.clients;

import api.language.Language;

public interface TranslationClient {

    String backend();
    String translate(String text, Language from, Language to);
}
