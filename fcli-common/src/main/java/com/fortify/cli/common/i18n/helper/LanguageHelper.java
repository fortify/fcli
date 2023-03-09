package com.fortify.cli.common.i18n.helper;

import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.FcliHomeHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

public final class LanguageHelper {
    // TODO Any way we can dynamically determine available languages?
    // TODO Re-add NL and other languages once resource bundles are up to date
    private static final String[] supportedLanguages = {"en", "nl"};
    private LanguageHelper() {}
    
    public static final Stream<LanguageDescriptor> getSupportLanguageDescriptorsStream() {
        return Stream.of(supportedLanguages)
            .map(LanguageDescriptor::new);
    }
    
    public static final LanguageDescriptor getConfiguredLanguageDescriptor() {
        Path languageConfigPath = getLanguageConfigPath();
        LanguageConfigDescriptor configDescriptor = !FcliHomeHelper.exists(languageConfigPath) 
                ? new LanguageConfigDescriptor() 
                : FcliHomeHelper.readFile(languageConfigPath, LanguageConfigDescriptor.class, true);
        return configDescriptor.getLanguageDescriptor();
    }
    
    public static final LanguageDescriptor setConfiguredLanguage(LanguageDescriptor descriptor) {
        Path languageConfigPath = getLanguageConfigPath();
        FcliHomeHelper.saveFile(languageConfigPath, new LanguageConfigDescriptor(descriptor), true);
        return descriptor;
    }
    
    public static final LanguageDescriptor setConfiguredLanguage(String language) {
        return setConfiguredLanguage(new LanguageDescriptor(language));
    }
    
    public static final void clearLanguageConfig() {
        FcliHomeHelper.deleteFile(getLanguageConfigPath(), true);
    }
    
    private static final Path getLanguageConfigPath() {
        return FcliHomeHelper.getFcliConfigPath().resolve("i18n/language.json");
    }
    
    @Data @EqualsAndHashCode(callSuper = false) 
    @NoArgsConstructor @AllArgsConstructor @ReflectiveAccess
    private static final class LanguageConfigDescriptor extends JsonNodeHolder {
        private String language = "en";
        
        public LanguageConfigDescriptor(LanguageDescriptor languageDescriptor) {
            this.language = languageDescriptor.getLanguage();
        }
        
        @JsonIgnore
        public LanguageDescriptor getLanguageDescriptor() {
            return new LanguageDescriptor(language);
        }
    }
}
