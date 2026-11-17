/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ai_assist.mcp.helper.arg;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.exception.FcliSimpleException;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

/**
 * Interface for defining MCP tool arguments
 *
 * @author Ruud Senden
 */
public interface IMCPToolArgHandler {
    /**
     * This method allows implementations to add MCP tool arguments and related data to the given {@link JsonSchema}.
     */
    public void updateSchema(JsonSchema schema);
    
    /**
     * Generate a legacy command string. This is retained for compatibility and must not be used
     * for untrusted values; implement {@link #getFcliArgs(Map)} for literal argument forwarding.
     */
    @Deprecated
    public String getFcliCmdArgs(Map<String, Object> toolArgs);

    /**
     * Generate individual fcli arguments based on the given MCP tool arguments. Legacy handlers
     * retain their existing command-string parsing behavior until they override this method.
     */
    default List<String> getFcliArgs(Map<String, Object> toolArgs) {
        var legacyArgs = getFcliCmdArgs(toolArgs);
        if ( legacyArgs==null ) { throw new FcliSimpleException("Legacy MCP argument handler returned null"); }
        return Arrays.asList(FcliCommandExecutorFactory.parseCommand("fcli "+legacyArgs));
    }

    static String toLegacyCmdArgs(List<String> args) {
        return args.stream().map(arg->"\""+arg+"\"").collect(Collectors.joining(" "));
    }

    static Stream<String> streamValues(Object value) {
        Stream<?> values;
        if ( value==null ) {
            values = Stream.empty();
        } else if ( value.getClass().isArray() ) {
            values = IntStream.range(0, Array.getLength(value)).mapToObj(i->Array.get(value, i));
        } else if ( value instanceof Collection<?> collection ) {
            values = collection.stream();
        } else {
            values = Stream.of(value);
        }
        return values.filter(Objects::nonNull).map(Object::toString);
    }
}