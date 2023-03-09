package com.fortify.cli.common.i18n.helper;

import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @ReflectiveAccess @AllArgsConstructor
public final class LanguageDescriptor {
    private final Locale locale;
    
    public LanguageDescriptor(String languageCode) {
        this(new Locale(languageCode));
    }

    public final boolean isActive() {
        return LanguageHelper.getConfiguredLanguageDescriptor().getLanguage().equals(getLanguage());
    }
    
    public final String getLanguage() {
        return locale.getLanguage();
    }
    
    public final String getLanguageLocalName() {
        return locale.getDisplayLanguage(locale);
    }
    
    public final String getLanguageName() {
        return locale.getDisplayLanguage(new Locale("en"));
    }
    
    public final ObjectNode asObjectNode() {
        return new ObjectMapper().valueToTree(this);
    }
}
