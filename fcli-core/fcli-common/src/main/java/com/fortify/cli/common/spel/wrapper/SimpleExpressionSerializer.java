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
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * This Jackson serializer allows for serializing {@link SimpleExpression} objects
 * to expression strings
 */
@Reflectable
public final class SimpleExpressionSerializer extends StdSerializer<SimpleExpression> {
    private static final long serialVersionUID = 1L;
    public SimpleExpressionSerializer() {
        super(SimpleExpression.class);
    }

    @Override
    public void serialize(SimpleExpression value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getExpressionString());
    }
}
