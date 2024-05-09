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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

public class GenerateActionSchema {
    public static void main(String[] args) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider((type, context) -> {
            if (type.getErasedType() != TemplateExpression.class) {
                return null;
            }
            ObjectNode customPatternSchema = context.getGeneratorConfig().createObjectNode()
                    .put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_STRING))
                    .put(context.getKeyword(SchemaKeyword.TAG_FORMAT), "spelTemplateExpression");
            return new CustomDefinition(customPatternSchema, true);
        });
        JacksonModule jacksonModule = new JacksonModule();
        SchemaGeneratorConfig config = configBuilder
                .with(jacksonModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .with(Option.FLATTENED_ENUMS)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(Action.class);
        Files.writeString(Path.of("/home/rsenden/fcli-action-schema.json"), jsonSchema.toPrettyString());
        System.out.println(jsonSchema.toPrettyString());
    }
}
