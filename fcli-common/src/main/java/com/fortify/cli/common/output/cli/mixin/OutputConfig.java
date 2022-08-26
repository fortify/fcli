/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.output.cli.mixin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputMixin.IDefaultFieldNameFormatterProvider;
import com.fortify.cli.common.output.transform.IJsonNodeTransformer;
import com.fortify.cli.common.output.transform.fields.PredefinedFieldsTransformerFactory;
import com.fortify.cli.common.output.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.output.transform.identity.IdentityTransformer;
import com.fortify.cli.common.output.writer.OutputFormat;
import com.fortify.cli.common.output.writer.OutputFormat.OutputType;

import io.micronaut.core.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
// TODO Add null checks in case any input or record transformation returns null?
public class OutputConfig {
    @Getter @Setter private OutputFormat defaultFormat;
    private final LinkedHashMap<Function<OutputFormat, Boolean>, String> defaultFields = new LinkedHashMap<>();
    private final List<BiFunction<OutputFormat,JsonNode,JsonNode>> inputTransformers = new ArrayList<>();
    private final List<BiFunction<OutputFormat,JsonNode,JsonNode>> recordTransformers = new ArrayList<>();
    
    public final OutputConfig inputTransformer(final Function<OutputFormat, Boolean> applyIf, final UnaryOperator<JsonNode> transformer) {
        inputTransformers.add((fmt,o)->!applyIf.apply(fmt) ? o : transformer.apply(o));
        return this;
    }
    
    public final OutputConfig inputTransformer(UnaryOperator<JsonNode> transformer) {
        return inputTransformer(fmt->true, transformer);
    }
    
    public final OutputConfig recordTransformer(Function<OutputFormat, Boolean> applyIf, UnaryOperator<JsonNode> transformer) {
        recordTransformers.add((fmt,o)->!applyIf.apply(fmt) ? o : transformer.apply(o));
        return this;
    }
    
    public final OutputConfig recordTransformer(UnaryOperator<JsonNode> transformer) {
        return recordTransformer(fmt->true, transformer);
    }
    
    public final OutputConfig defaultFields(Function<OutputFormat, Boolean> applyIf, String fields) {
        defaultFields.put(applyIf, fields);
        return this;
    }
    
    public final OutputConfig defaultFields(String fields) {
        defaultFields.put(fmt->true, fields);
        return this;
    }
    
    public final OutputConfig defaultColumns(String outputColumns) {
        return defaultFields(OutputFormat::isColumns, outputColumns);
    }
    
    final JsonNode applyFieldsTransformations(OutputFormat outputFormat, String overrideFieldsString, IDefaultFieldNameFormatterProvider defaultFieldNameFormatterProvider, JsonNode input) {
        if ( StringUtils.isNotEmpty(overrideFieldsString) ) {
            return applyDefaultFieldsTransformations(outputFormat, overrideFieldsString, defaultFieldNameFormatterProvider, input);
        } else {
            return applyDefaultFieldsTransformations(defaultFields, outputFormat, defaultFieldNameFormatterProvider, input);
        }
    }
    
    private final JsonNode applyDefaultFieldsTransformations(OutputFormat outputFormat, String fieldsString, IDefaultFieldNameFormatterProvider defaultFieldNameFormatterProvider, JsonNode input) {
        LinkedHashMap<Function<OutputFormat, Boolean>, String> fields = new LinkedHashMap<>();
        fields.put(fmt->true, fieldsString);
        return applyDefaultFieldsTransformations(fields, outputFormat, defaultFieldNameFormatterProvider, input);
    }
    
    private final JsonNode applyDefaultFieldsTransformations(LinkedHashMap<Function<OutputFormat, Boolean>, String> fields, OutputFormat outputFormat, IDefaultFieldNameFormatterProvider defaultFieldNameFormatterProvider, JsonNode input) {
        return fields.entrySet().stream()
            .filter(e->e.getKey().apply(outputFormat))
            .map(Map.Entry::getValue)
            .map(fieldsString->getFieldsTransformer(outputFormat, fieldsString, defaultFieldNameFormatterProvider))
            .reduce(input, (o, t) -> t.transform(o), (m1, m2) -> m2);
    }

    private IJsonNodeTransformer getFieldsTransformer(OutputFormat outputFormat, String fieldsString, IDefaultFieldNameFormatterProvider defaultFieldNameFormatterProvider) {
        if ( StringUtils.isNotEmpty(fieldsString) && !"all".equals(fieldsString) ) {
            return PredefinedFieldsTransformerFactory.createFromString(defaultFieldNameFormatterProvider.getDefaultFieldNameFormatter(outputFormat), fieldsString);
        } else if ( outputFormat.getOutputType()==OutputType.TEXT_COLUMNS ) {
            return new FlattenTransformer(defaultFieldNameFormatterProvider.getDefaultFieldNameFormatter(outputFormat), ".", false);
        } else {
            return new IdentityTransformer();
        }
    }
    
    final JsonNode applyInputTransformations(OutputFormat outputFormat, JsonNode input) {
        return applyTransformations(inputTransformers, outputFormat, input);
    }
    
    final JsonNode applyRecordTransformations(OutputFormat outputFormat, JsonNode input) {
        return applyTransformations(recordTransformers, outputFormat, input);
    }
    
    private final JsonNode applyTransformations(List<BiFunction<OutputFormat, JsonNode, JsonNode>> transformations, OutputFormat outputFormat, JsonNode input) {
        return transformations.stream()
                .reduce(input, (o, t) -> t.apply(outputFormat, o), (m1, m2) -> m2);
    }
    
    public static final OutputConfig csv() {
        return new OutputConfig().defaultFormat(OutputFormat.csv);
    }
    
    public static final OutputConfig json() {
        return new OutputConfig().defaultFormat(OutputFormat.json);
    }
    
    public static final OutputConfig table() {
        return new OutputConfig().defaultFormat(OutputFormat.table);
    }
    
    public static final OutputConfig tree() {
        return new OutputConfig().defaultFormat(OutputFormat.tree);
    }
    
    public static final OutputConfig xml() {
        return new OutputConfig().defaultFormat(OutputFormat.xml);
    }
    
    public static final OutputConfig yaml() {
        return new OutputConfig().defaultFormat(OutputFormat.yaml);
    }
}
