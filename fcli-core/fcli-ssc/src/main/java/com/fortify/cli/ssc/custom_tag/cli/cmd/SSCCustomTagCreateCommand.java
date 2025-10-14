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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagValueType;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCCustomTagCreateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Parameters(index = "0", paramLabel = "NAME", descriptionKey = "fcli.ssc.custom-tag.create.name")
    private String name;
    @Option(names = {"-t", "--type"}, required = true)
    private SSCCustomTagValueType valueType;
    @Option(names = {"-d", "--description"}, required = false)
    private String description;
    @Option(names = {"--values"}, required = false)
    private String values;
    @Option(names = {"--restricted"}, required = false, negatable = true, defaultValue = "false")
    private boolean restriction;
    @Option(names = {"--hidden"}, required = false, negatable = true, defaultValue = "false")
    private boolean hidden;
    @Option(names = {"--requires-comment"}, required = false, negatable = true, defaultValue = "false")
    private boolean requiresComment;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ObjectNode body = buildBody();
        var response = unirest.post("/api/v1/customTags")
            .body(body)
            .asObject(JsonNode.class).getBody();
        return response;
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    // --- Private body-building helpers below ---
    private ObjectNode buildBody() {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("name", name);
        body.put("description", description != null ? description : "");
        body.put("valueType", valueType.name());
        body.put("restriction", restriction);
        body.put("hidden", hidden);
        body.put("requiresComment", requiresComment);
        body.put("customTagType", "CUSTOM");
        if (valueType == SSCCustomTagValueType.LIST) {
            body.put("extensible", true);
            body.set("valueList", buildValueList());
        }
        return body;
    }

    private com.fasterxml.jackson.databind.node.ArrayNode buildValueList() {
        if (values == null || values.isBlank()) {
            throw new FcliSimpleException("At least one value must be specified for LIST type using --values");
        }
        var valueList = JsonNodeFactory.instance.arrayNode();
        String[] vals = values.split(",");
        for (int i = 0; i < vals.length; i++) {
            String val = vals[i].trim();
            ObjectNode entry = JsonNodeFactory.instance.objectNode();
            entry.put("lookupIndex", i+1);
            entry.put("deletable", true);
            entry.put("lookupValue", val);
            entry.put("description", "");
            entry.putNull("auditAssistantTrainingLabel");
            entry.put("hidden", false);
            entry.put("seqNumber", i+1);
            valueList.add(entry);
        }
        return valueList;
    }
}