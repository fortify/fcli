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
package com.fortify.cli.ssc.custom_tag.cli.cmd;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.custom_tag.cli.mixin.SSCCustomTagResolverMixin;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDescriptor;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class SSCCustomTagUpdateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private SSCCustomTagResolverMixin.PositionalParameterSingle customTagResolver;
    @Option(names = {"--name"}, required = false)
    private String name;
    @Option(names = {"-d", "--description"}, required = false)
    private String description;
    @Option(names = {"--values"}, required = false)
    private String values;
    @Option(names = {"--add-values"}, required = false)
    private String addValues;
    @Option(names = {"--rm-values"}, required = false)
    private String rmValues;
    @Option(names = {"--restricted"}, required = false, negatable = true)
    private Boolean restriction;
    @Option(names = {"--hidden"}, required = false, negatable = true)
    private Boolean hidden;
    @Option(names = {"--requires-comment"}, required = false, negatable = true)
    private Boolean requiresComment;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCCustomTagDescriptor desc = customTagResolver.getCustomTagDescriptor(unirest);
        ObjectNode updateData = buildBody(desc);
        unirest.put("/api/v1/customTags/{id}")
            .routeParam("id", desc.getId())
            .body(updateData).asObject(JsonNode.class).getBody();
        return new SSCCustomTagHelper(unirest).getDescriptorByCustomTagSpec(desc.getGuid(), true).asJsonNode();
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    // --- Private body-building helpers below ---
    private ObjectNode buildBody(SSCCustomTagDescriptor desc) {
        ObjectNode body = (ObjectNode)desc.asJsonNode().deepCopy();
        if (name != null && !name.isBlank()) { body.put("name", name); }
        if (description != null) { body.put("description", description); }
        if (restriction != null) { body.put("restriction", restriction); }
        if (hidden != null) { body.put("hidden", hidden); }
        if (requiresComment != null) { body.put("requiresComment", requiresComment); }
        if ("LIST".equalsIgnoreCase(body.path("valueType").asText())) {
            body.set("valueList", buildValueList(body));
        }
        return body;
    }

    private ArrayNode buildValueList(ObjectNode body) {
        LinkedHashMap<String, ObjectNode> valueMap = buildValueMap(body);
        if (values != null) {
            valueMap.clear();
            addValuesToMap(valueMap, values);
        }
        if (addValues != null) {
            addValuesToMap(valueMap, addValues);
        }
        if (rmValues != null) {
            removeValuesFromMap(valueMap, rmValues);
        }
        if (valueMap.isEmpty()) {
            throw new FcliSimpleException("At least one value must be specified for LIST type");
        }
        var newValueList = JsonNodeFactory.instance.arrayNode();
        int idx = 1;
        for (ObjectNode entry : valueMap.values()) {
            entry.put("lookupIndex", idx);
            entry.put("seqNumber", idx);
            newValueList.add(entry);
            idx++;
        }
        return newValueList;
    }

    private LinkedHashMap<String, ObjectNode> buildValueMap(ObjectNode body) {
        var valueList = body.withArray("valueList");
        LinkedHashMap<String, ObjectNode> valueMap = new LinkedHashMap<>();
        for (JsonNode v : valueList) {
            valueMap.put(v.path("lookupValue").asText(), (ObjectNode)v);
        }
        return valueMap;
    }

    private void addValuesToMap(LinkedHashMap<String, ObjectNode> valueMap, String valuesStr) {
        String[] vals = valuesStr.split(",");
        for (String val : vals) {
            val = val.trim();
            if (!valueMap.containsKey(val)) {
                ObjectNode entry = JsonNodeFactory.instance.objectNode();
                entry.put("lookupValue", val);
                entry.put("deletable", true);
                entry.put("description", "");
                entry.putNull("auditAssistantTrainingLabel");
                entry.put("hidden", false);
                valueMap.put(val, entry);
            }
        }
    }

    private void removeValuesFromMap(LinkedHashMap<String, ObjectNode> valueMap, String valuesStr) {
        String[] vals = valuesStr.split(",");
        for (String val : vals) {
            val = val.trim();
            valueMap.remove(val);
        }
    }
}