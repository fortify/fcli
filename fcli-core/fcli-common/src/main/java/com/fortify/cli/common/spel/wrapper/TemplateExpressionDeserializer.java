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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.SpelHelper;

/**
 * This Jackson deserializer allows got parsing String values into 
 * TemplateExpression objects.
 */
@Reflectable
public final class TemplateExpressionDeserializer extends StdDeserializer<TemplateExpression> {
    private static final long serialVersionUID = 1L;
    
    public TemplateExpressionDeserializer() { this(null); } 
    public TemplateExpressionDeserializer(Class<?> vc) { super(vc); }

    @Override
    public TemplateExpression deserialize(JsonParser jp, DeserializationContext ctxt) 
    throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        return node==null || node.isNull() ? null : SpelHelper.parseTemplateExpression(node.asText());
    }
}
