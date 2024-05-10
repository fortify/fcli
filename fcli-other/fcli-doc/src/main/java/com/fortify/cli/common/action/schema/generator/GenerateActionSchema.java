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
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
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
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider((type, context) -> {
            if (type.getErasedType() == TemplateExpression.class) {
                var custom = context.getGeneratorConfig().createObjectNode();
                custom.put(context.getKeyword(SchemaKeyword.TAG_FORMAT), "spelTemplateExpression")
                    .putArray(context.getKeyword(SchemaKeyword.TAG_TYPE))
                        .add(context.getKeyword(SchemaKeyword.TAG_TYPE_BOOLEAN))
                        .add(context.getKeyword(SchemaKeyword.TAG_TYPE_INTEGER))
                        .add(context.getKeyword(SchemaKeyword.TAG_TYPE_STRING))
                        .add(context.getKeyword(SchemaKeyword.TAG_TYPE_NUMBER));
                return new CustomDefinition(custom, true);
            } else if (type.getErasedType() == JsonNode.class ) {
                var custom = context.getGeneratorConfig().createObjectNode();
                custom.put(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES), true)
                    .putArray(context.getKeyword(SchemaKeyword.TAG_TYPE))
                        .add(context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                        .add(context.getKeyword(SchemaKeyword.TAG_TYPE_STRING));
                return new CustomDefinition(custom, true);
            } else {
                return null;
            }
        });
        SchemaGeneratorConfig config = configBuilder
                .with(jacksonModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.FLATTENED_ENUMS)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                // TODO Ideally we'd also want to support all other primitive types,
                //      for example to allow 'if: true' in addition to 'if: "true"'
                //      or 'if: ${true}'
                //.with(new SimpleTypeModule().withStandardStringType(SimpleExpression.class, "spel-expression"))
                //.with(new SimpleTypeModule().withStandardStringType(TemplateExpression.class, "spel-template-expression"))
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
