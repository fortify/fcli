/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.app.helper.FoDAppUpdateRequest;
import com.fortify.cli.fod.app.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDUpdateCommand;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Map;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Update.CMD_NAME)
public class FoDAppUpdateCommand extends AbstractFoDUpdateCommand  {

    @Mixin private FoDAppResolverMixin.PositionalParameter appResolver;

    @Option(names = {"--name,", "-n"}, descriptionKey = "appName")
    private String applicationNameUpdate;
    @Option(names = {"--description", "-d"}, descriptionKey = "appDesc")
    private String descriptionUpdate;
    @Option(names = {"--notify"}, arity = "0..*", descriptionKey = "")
    private ArrayList<String> notificationsUpdate;

    @Mixin
    private FoDCriticalityTypeOptions.OptionalCritOption criticalityTypeUpdate;
    @Mixin
    private FoDAttributeUpdateOptions.OptionalAttrOption appAttrsUpdate;

    @SneakyThrows
    @Override
    protected Void run(UnirestInstance unirest) {

        // current values of app being updated
        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, appResolver.getAppNameOrId(), true);
        ArrayList<FoDAttributeDescriptor> appAttrsCurrent = appDescriptor.getAttributes();

        // new values to replace
        FoDCriticalityTypeOptions.FoDCriticalityType appCriticalityNew = criticalityTypeUpdate.getCriticalityType();
        Map<String, String> attributeUpdates = appAttrsUpdate.getAttributes();
        JsonNode jsonAttrs = getObjectMapper().createArrayNode();
        if (attributeUpdates != null && attributeUpdates.size() > 0) {
            jsonAttrs = FoDAttributeHelper.mergeAttributesNode(unirest, appAttrsCurrent, attributeUpdates);
        } else {
            jsonAttrs = FoDAttributeHelper.getAttributesNode(appAttrsCurrent);
        }
        String appEmailListNew = FoDAppHelper.getEmailList(notificationsUpdate);

        FoDAppUpdateRequest appUpdateRequest = new FoDAppUpdateRequest()
                .setApplicationName(StringUtils.isNotEmpty(applicationNameUpdate) ? applicationNameUpdate : appDescriptor.getApplicationName())
                .setApplicationDescription(StringUtils.isNotEmpty(descriptionUpdate) ? descriptionUpdate : appDescriptor.getApplicationDescription())
                .setBusinessCriticalityType(appCriticalityNew != null ? String.valueOf(appCriticalityNew) : appDescriptor.getBusinessCriticalityType())
                .setEmailList(StringUtils.isNotEmpty(appEmailListNew) ? appEmailListNew : appDescriptor.getEmailList())
                .setAttributes(jsonAttrs);

        FoDAppDescriptor result = FoDAppHelper.updateApp(unirest, appDescriptor.getApplicationId(), appUpdateRequest);
        getOutputMixin().write(result.asObjectNode());
        return null;
    }

    @Override
    protected JsonNode generateOutput(UnirestInstance unirest) {
        return appResolver.getAppDescriptor(unirest).asJsonNode();
    }

    @Override
    protected JsonNode transformRecord(JsonNode record) {
        return FoDAppHelper.renameFields(record);
    }

}
