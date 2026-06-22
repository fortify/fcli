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
package com.fortify.cli.fod.attribute.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDefinitionDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDefinitionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAttributeResolverMixin {

    public static abstract class AbstractFoDAttributeResolverMixin {
        public abstract String getAttributeId();

        public FoDAttributeDefinitionDescriptor getAttributeDescriptor(UnirestInstance unirest) {
            return new FoDAttributeDefinitionHelper(unirest).getDefinition(getAttributeId(), true);
        }
    }

    public static abstract class AbstractFoDMultiAttributeResolverMixin {
        public abstract String[] getAttributeIds();

        public FoDAttributeDefinitionDescriptor[] getAttributeDescriptors(UnirestInstance unirest) {
            var helper = new FoDAttributeDefinitionHelper(unirest);
            return Stream.of(getAttributeIds())
                    .map(id -> helper.getDefinition(id, true))
                    .toArray(FoDAttributeDefinitionDescriptor[]::new);
        }

        public Collection<JsonNode> getAttributeDescriptorJsonNodes(UnirestInstance unirest) {
            return Stream.of(getAttributeDescriptors(unirest))
                    .map(FoDAttributeDefinitionDescriptor::asJsonNode)
                    .collect(Collectors.toList());
        }

        public Integer[] getAttributeIds(UnirestInstance unirest) {
            return Stream.of(getAttributeDescriptors(unirest))
                    .map(FoDAttributeDefinitionDescriptor::getId)
                    .toArray(Integer[]::new);
        }
    }

    public static class RequiredOption extends AbstractFoDAttributeResolverMixin {
        @Option(names = {"--attribute-id"}, required = true)
        @Getter private String attributeId;
    }

    public static class RequiredOptionMulti extends AbstractFoDMultiAttributeResolverMixin {
        @Option(names = {"--attribute-ids"}, required = true, split = ",", descriptionKey = "fcli.fod.attribute.attribute-name-or-id")
        @Getter private String[] attributeIds;
    }

    public static class PositionalParameter extends AbstractFoDAttributeResolverMixin {
        @Parameters(index = "0", arity = "1", paramLabel = "attribute-id", descriptionKey = "fcli.fod.attribute.attribute-name-or-id")
        @Getter private String attributeId;
    }

    public static class PositionalParameterMulti extends AbstractFoDMultiAttributeResolverMixin {
        @Parameters(index = "0", arity = "1..", paramLabel = "attribute-id's", descriptionKey = "fcli.fod.attribute.attribute-name-or-id")
        @Getter private String[] attributeIds;
    }
}
