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

import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * This Jackson deserializer allows for parsing String values into 
 * {@link SimpleExpression} objects
 */
@Reflectable
public final class SimpleExpressionDeserializer extends StdDeserializer<SimpleExpression> {
    private static final long serialVersionUID = 1L;
    private static final SpelExpressionParser parser = new SpelExpressionParser();
    public SimpleExpressionDeserializer() { this(null); } 
    public SimpleExpressionDeserializer(Class<?> vc) { super(vc); }

    @Override
    public SimpleExpression deserialize(JsonParser jp, DeserializationContext ctxt) 
    throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        return node==null || node.isNull() ? null : new SimpleExpression(node.asText(), parser.parseExpression(node.asText()));
    }
}
