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
package com.fortify.cli.common.output.writer.output.standard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
// TODO Add null checks in case any input or record transformation returns null?
public class StandardOutputConfig {
    @Getter @Setter private RecordWriterFactory defaultFormat;
    private final List<Function<JsonNode,JsonNode>> inputTransformers = new ArrayList<>();
    private final List<Function<JsonNode,JsonNode>> recordTransformers = new ArrayList<>();
    
    public final StandardOutputConfig inputTransformer(UnaryOperator<JsonNode> transformer) {
        inputTransformers.add(transformer);
        return this;
    }
    
    public final StandardOutputConfig recordTransformer(UnaryOperator<JsonNode> transformer) {
        recordTransformers.add(transformer);
        return this;
    }
    
    final JsonNode applyInputTransformations(JsonNode input) {
        return applyTransformations(inputTransformers, input);
    }
    
    final JsonNode applyRecordTransformations(JsonNode record) {
        return applyTransformations(recordTransformers, record);
    }
    
    private final JsonNode applyTransformations(List<Function<JsonNode, JsonNode>> transformations, JsonNode input) {
        return transformations.stream()
                .reduce(input, (o, t) -> t.apply(o), (m1, m2) -> m2);
    }
    
    public static final StandardOutputConfig csv() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.csv);
    }
    
    public static final StandardOutputConfig json() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.json);
    }
    
    public static final StandardOutputConfig table() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.table);
    }
    
    public static final StandardOutputConfig xml() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.xml);
    }
    
    public static final StandardOutputConfig yaml() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.yaml);
    }

    public static final StandardOutputConfig details() {
        return yaml();
    }
}
