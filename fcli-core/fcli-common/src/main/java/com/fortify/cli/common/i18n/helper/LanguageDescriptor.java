/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.i18n.helper;

import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public final class LanguageDescriptor {
    private Locale locale;
    
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
