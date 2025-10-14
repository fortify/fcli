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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * This Jackson key deserializer allows for parsing map keys into 
 * TemplateExpression objects.
 */
@Reflectable
public final class TemplateExpressionKeySerializer extends JsonSerializer<TemplateExpression> {
    @Override
    public void serialize(TemplateExpression value, JsonGenerator jgen, SerializerProvider provider) 
        throws IOException, JsonProcessingException
    {
        jgen.writeFieldName(value==null ? "" : value.getOriginalExpressionString());
    }
    
    public static final ObjectMapper registerOn(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addKeySerializer(TemplateExpression.class, new TemplateExpressionKeySerializer());
        return objectMapper.registerModule(module);
    }
}
