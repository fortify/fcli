package com.fortify.cli.config.language.helper;

import java.util.Locale;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.config.common.helper.FcliConfigManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Data;

@Singleton
public final class LanguageConfigManager {
    // TODO Any way we can dynamically determine available languages?
    // TODO Re-add NL and other languages once resource bundles are up to date
    private static final String[] supportedLanguages = {"en", "nl"};
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
    
    public Stream<LanguageDescriptor> getSupportLanguageDescriptorsStream() {
        return Stream.of(supportedLanguages)
            .map(this::getLanguageDescriptor);
    }
    
    public LanguageDescriptor getCurrentLanguageDescriptor() {
        return getLanguageDescriptor(getLanguage());
    }

    public LanguageDescriptor getLanguageDescriptor(String langCode){
        Locale l = new Locale(langCode);
        return new LanguageDescriptor(l.getLanguage(), l.getDisplayLanguage(l), l.getDisplayLanguage(new Locale("en")));
    }

    public Locale getLocale() {
        return isNullEmptyOrEn() ? new Locale("") : new Locale(getLanguage());
    }
    
    @Data
    public final class LanguageDescriptor {
        private final String languageCode;
        private final String languageLocalName;
        private final String languageName;
    
        public final boolean isActive() {
            return getLanguage().equals(languageCode);
        }
        
        public final ObjectNode asObjectNode() {
            return new ObjectMapper().valueToTree(this);
        }
    }
}