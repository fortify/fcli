package com.fortify.cli.config.language.manager;

import java.util.Locale;

import com.fortify.cli.config._main.manager.FcliConfigManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class LanguageConfigManager {
    public static final String[] supportedLanguages = {"en","nl"};
    private static final String CONFIG_KEY = "defaultUserLanguage";
    private final FcliConfigManager config;

    @Inject
    public LanguageConfigManager(FcliConfigManager config) {
        this.config = config;
    }

    private static final boolean isNullEmptyOrEn(String lang){
        return lang == null || lang.isEmpty() || lang.toLowerCase() == "en";
    }

    public final boolean isNullEmptyOrEn(){
        return isNullEmptyOrEn(config.get(CONFIG_KEY));
    }

    public final String getLanguage(){
        return !isNullEmptyOrEn() ? config.get(CONFIG_KEY) : "en";
    }

    public void setLanguage(String lang){
        if(isNullEmptyOrEn(lang)){
           lang = "";       // done this way to force the locale/language to the "default locale" which is English.
        }
        config.set(CONFIG_KEY, lang);
        config.save();
    }

    public String getLanguageForHelp(String langCode){
        Locale l = new Locale(langCode);
        String f = "%s - %s (%s)";
        return String.format(f,
                l.getLanguage(),                                // 2-letter language code.
                l.getDisplayLanguage(l),                        // Readable name of language in language's own script.
                l.getDisplayLanguage(new Locale("en"))  // Readable name of language in English.
        );
    }

	public Locale getLocale() {
		return isNullEmptyOrEn() ? new Locale("") : new Locale(getLanguage());
	}
}