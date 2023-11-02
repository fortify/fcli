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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppAndVersionNameResolverMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCCopyFromAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.HashMap;

@Command(name = "copy-issues")
public class SSCAppVersionCopyStateCommand extends AbstractSSCJsonNodeOutputCommand implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter
    @Mixin
    private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin
    private SSCAppAndVersionNameResolverMixin.PositionalParameter sscAppAndVersionNameResolver;
    @Mixin
    private SSCCopyFromAppVersionResolverMixin.RequiredOption fromAppVersionResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ObjectMapper mapper = new ObjectMapper();
        SSCAppVersionDescriptor targetAppVersionDescriptor = SSCAppVersionHelper.getRequiredAppVersion(unirest, sscAppAndVersionNameResolver.getAppAndVersionName(), sscAppAndVersionNameResolver.getDelimiter());
        SSCAppVersionDescriptor sourceAppVersionDescriptor = fromAppVersionResolver.getAppVersionDescriptor(unirest, sscAppAndVersionNameResolver.getDelimiter());

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
