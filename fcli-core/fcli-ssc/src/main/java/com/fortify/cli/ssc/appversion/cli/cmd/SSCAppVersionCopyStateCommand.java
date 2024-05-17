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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionRefreshOptions;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCDelimiterMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "copy-state")
public class SSCAppVersionCopyStateCommand extends AbstractSSCJsonNodeOutputCommand implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter
    @Mixin
    private OutputHelperMixins.TableNoQuery outputHelper;
    @Getter
    @Mixin
    private SSCDelimiterMixin delimiterMixin;
    @Mixin private SSCAppVersionRefreshOptions refreshOptions;
    @Option(names = {"--copy-from", "--from"}, required = true, descriptionKey = "fcli.ssc.appversion.resolver.copy-from.nameOrId")
    @Getter
    private String fromAppVersionNameOrId;
    @Option(names = {"--copy-to", "--to"}, required = true, descriptionKey = "fcli.ssc.appversion.resolver.copy-to.nameOrId")
    @Getter
    private String toAppVersionNameOrId;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ObjectMapper mapper = new ObjectMapper();
        SSCAppVersionDescriptor fromAppVersionDescriptor = SSCAppVersionHelper.getRequiredAppVersion(unirest, getFromAppVersionNameOrId(), delimiterMixin.getDelimiter());
        SSCAppVersionDescriptor toAppVersionDescriptor = SSCAppVersionHelper.getRequiredAppVersion(unirest, getToAppVersionNameOrId(), delimiterMixin.getDelimiter());

        if(refreshOptions.isRefresh() && fromAppVersionDescriptor.isRefreshRequired()){
            SSCJobDescriptor refreshJobDesc = SSCAppVersionHelper.refreshMetrics(unirest, fromAppVersionDescriptor);
            SSCJobHelper.waitForJob(unirest,refreshJobDesc);
        }

        return mapper.valueToTree(copyState(unirest, fromAppVersionDescriptor, toAppVersionDescriptor));

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

    private static final JsonNode copyState(UnirestInstance unirest, SSCAppVersionDescriptor fromAppVersionDescriptor, SSCAppVersionDescriptor toAppVersionDescriptor) {
        ObjectNode copyStateOptions =  JsonHelper.getObjectMapper().createObjectNode();
        copyStateOptions.put("previousProjectVersionId", fromAppVersionDescriptor.getIntVersionId());
        copyStateOptions.put("projectVersionId", toAppVersionDescriptor.getIntVersionId());

        ObjectNode body = JsonHelper.getObjectMapper().createObjectNode();
        body    .put("type", "copy_current_state")
                .set("values", copyStateOptions);

        unirest .post(SSCUrls.PROJECT_VERSIONS_ACTION(toAppVersionDescriptor.getVersionId()))
                .body(body)
                .asObject(JsonNode.class).getBody();

        return copyStateOptions;
    }
}
