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
package com.fortify.cli.fod.app.cli.cmd;

import static com.fortify.cli.common.util.DisableTest.TestType.MULTI_OPT_PLURAL_NAME;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.app.attr.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.app.attr.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.app.attr.helper.FoDAttributeHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.app.helper.FoDAppUpdateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDAppUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppResolverMixin.PositionalParameter appResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = {"--name", "-n"})
    private String applicationNameUpdate;
    @Option(names = {"--description", "-d"})
    private String descriptionUpdate;
    @DisableTest(MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--notify"}, required = false, split=",")
    private ArrayList<String> notificationsUpdate;
    @Mixin private FoDCriticalityTypeOptions.OptionalOption criticalityTypeUpdate;
    @Mixin private FoDAttributeUpdateOptions.OptionalAttrOption appAttrsUpdate;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        // current values of app being updated
        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, appResolver.getAppNameOrId(), true);
        ArrayList<FoDAttributeDescriptor> appAttrsCurrent = appDescriptor.getAttributes();

        // new values to replace
        FoDCriticalityTypeOptions.FoDCriticalityType appCriticalityNew = criticalityTypeUpdate.getCriticalityType();
        Map<String, String> attributeUpdates = appAttrsUpdate.getAttributes();
        JsonNode jsonAttrs = objectMapper.createArrayNode();
        if (attributeUpdates != null && !attributeUpdates.isEmpty()) {
            jsonAttrs = FoDAttributeHelper.mergeAttributesNode(unirest, FoDEnums.AttributeTypes.Application, appAttrsCurrent, 
                attributeUpdates);
        } else {
            jsonAttrs = FoDAttributeHelper.getAttributesNode(FoDEnums.AttributeTypes.Application, appAttrsCurrent);
        }
        String appEmailListNew = FoDAppHelper.getEmailList(notificationsUpdate);

        FoDAppUpdateRequest appUpdateRequest = FoDAppUpdateRequest.builder()
                .applicationName(StringUtils.isNotBlank(applicationNameUpdate) ? applicationNameUpdate : appDescriptor.getApplicationName())
                .applicationDescription(StringUtils.isNotBlank(descriptionUpdate) ? descriptionUpdate : appDescriptor.getApplicationDescription())
                .businessCriticalityType(appCriticalityNew != null ? String.valueOf(appCriticalityNew) : appDescriptor.getBusinessCriticalityType())
                .emailList(StringUtils.isNotBlank(appEmailListNew) ? appEmailListNew : appDescriptor.getEmailList())
                .attributes(jsonAttrs).build();

        return FoDAppHelper.updateApp(unirest, appDescriptor.getApplicationId(), appUpdateRequest).asJsonNode();
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppHelper.transformRecord(record);
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

}
