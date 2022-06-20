package com.fortify.cli.common.locale;

import com.fortify.cli.common.config.FcliConfig;
import com.fortify.cli.common.config.IFortifyCLIInitializer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;

@Singleton
public class LanguageHelper implements IFortifyCLIInitializer {
    public static final String[] supportedLanguages = {"en","nl","zh"};
    private static final String CONFIG_KEY = "defaultUserLanguage";
    private final FcliConfig config;

    @Inject
    public LanguageHelper(FcliConfig config) {
        this.config = config;
    }

    public boolean isNullEmptyOrEn(String lang){
        boolean t = false;
        if(lang == null || lang.isEmpty() || lang.toLowerCase() == "en"){
            t = true;
        }
        return t;
    }

    public boolean isNullEmptyOrEn(){
        return isNullEmptyOrEn(config.get(CONFIG_KEY));
    }

    public String getLanguageConfig(){
        return !isNullEmptyOrEn() ? config.get(CONFIG_KEY) : "en";
    }

    public void setLanguageConfig(String lang){
        if(isNullEmptyOrEn(lang)){
           lang = "";       // done this way to force the locale/language to the "default locale" which is English.
        }
        config.set(CONFIG_KEY, lang);
    }

    public void initializeLanguage() {
        if(isNullEmptyOrEn()){
            Locale.setDefault(new Locale(""));
            return;
        }
        Locale.setDefault(new Locale(getLanguageConfig()));
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

    @Override
    public void initializeFortifyCLI(String[] args) {
        initializeLanguage();
    }
}