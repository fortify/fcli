/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.spel.wrapper;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.SpelHelper;

/**
 * This Jackson key deserializer allows for parsing map keys into 
 * TemplateExpression objects.
 */
@Reflectable
public final class TemplateExpressionKeyDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return key==null ? null : SpelHelper.parseTemplateExpression(key);
    }
    
    public static final ObjectMapper registerOn(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(TemplateExpression.class, new TemplateExpressionKeyDeserializer());
        return objectMapper.registerModule(module);
    }
}
