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
package com.fortify.cli.ssc.appversion.cli.cmd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCSourceAndTargetAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.attribute.domain.SSCAttributeDefinitionType;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeOptionDefinitionDescriptor;
import kong.unirest.UnirestInstance;
import kong.unirest.json.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Command(name = "copy-state")
public class SSCAppVersionCopyStateCommand extends AbstractSSCJsonNodeOutputCommand implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter
    @Mixin
    private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin
    private SSCSourceAndTargetAppVersionResolverMixin.RequiredOption appVersionsResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ObjectMapper mapper = new ObjectMapper();
        SSCAppVersionDescriptor sourceAppVersionDescriptor = appVersionsResolver.getSourceAppVersionDescriptor(unirest);
        SSCAppVersionDescriptor targetAppVersionDescriptor = appVersionsResolver.getTargetAppVersionDescriptor(unirest);

        return mapper.valueToTree(copyState(unirest, sourceAppVersionDescriptor, targetAppVersionDescriptor));

    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "COPY_REQUESTED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    private static final HashMap<String, String> copyState(UnirestInstance unirest, SSCAppVersionDescriptor sourceAppVersion, SSCAppVersionDescriptor targetAppVersion) {
        HashMap<String, String> appVersionIds = new HashMap<String, String>();
        appVersionIds.put("previousProjectVersionId", sourceAppVersion.getVersionId());
        appVersionIds.put("projectVersionId", targetAppVersion.getVersionId());
        String action = "COPY_REQUESTED";
        try {
            JsonNode response = unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_COPY_CURRENT_STATE)
                    .body(appVersionIds)
                    .asObject(JsonNode.class).getBody();
            if (response.has("responseCode")) {
                if (response.get("responseCode").intValue() != 200) {
                    action = "UNKNOWN";
                }
            }
        } catch (UnexpectedHttpResponseException e) {
            action = "COPY_FAILED";
        }

        appVersionIds.put(IActionCommandResultSupplier.actionFieldName, action);
        return appVersionIds;
    }

}
