/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.schema.generator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.SupportedSchemaVersion;
import com.fortify.cli.common.spring.expression.wrapper.SimpleExpression;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.impl.module.SimpleTypeModule;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

public class GenerateActionSchema {
    public static void main(String[] args) throws Exception {
        if ( args.length!=1 ) { throw new IllegalArgumentException("Output directory must be specified as single argument"); }
        var outputPath = Path.of(args[0]);
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        JacksonModule jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED, JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY);
        configBuilder.forFields().withTargetTypeOverridesResolver(field->{
            if ( field.getName().equals("contents") && field.getType().isInstanceOf(JsonNode.class) ) {
                var context = field.getContext();
                return Stream.of(ObjectNode.class, String.class)
                    .map(context::resolve)
                    .collect(Collectors.toList());
            }
            return null;
        });
        SchemaGeneratorConfig config = configBuilder
                .with(jacksonModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.FLATTENED_ENUMS)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .with(new SimpleTypeModule().withStandardStringType(SimpleExpression.class, "spel-expression"))
                .with(new SimpleTypeModule().withStandardStringType(TemplateExpression.class, "spel-template-expression"))
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(Action.class);
        Files.createDirectories(outputPath);
        var outputFile = outputPath.resolve(String.format("fcli-action-schema-%s.json", SupportedSchemaVersion.current.toString()));
        Files.writeString(outputFile, jsonSchema.toPrettyString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Fortify CLI action schema written to "+outputFile.toString());
    }
    /*
    private static final class JsonNodeSubTypeResolver implements SubtypeResolver {
        @Override
        public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
            if ( declaredType.isInstanceOf(JsonNode.class) ) {
                var typeContext = context.getTypeContext();
                return Stream.of(ObjectNode.class, TextNode.class)
                        .map(c->typeContext.resolveSubtype(declaredType, c))
                        .collect(Collectors.toList());
            }
            return null;
        }
        
    }
    */
}
