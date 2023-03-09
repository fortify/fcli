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

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppCreateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;
    @Spec CommandSpec spec;
    //ResourceBundle bundle = ResourceBundle.getBundle("com.fortify.cli.fod.i18n.FoDMessages");

    @Parameters(index = "0", arity = "1", descriptionKey = "application-name")
    private String applicationName;
    @Parameters(index = "1", arity = "1", descriptionKey = "release-name")
    private String releaseName;
    @Option(names = {"--description", "-d"})
    private String description;
    @Option(names = {"--notify"}, required = false, arity = "0..*")
    private ArrayList<String> notifications;
    @Option(names = {"--release-description", "--rel-desc"})
    private String releaseDescription;
    @Option(names = {"--owner"}, required = true)
    private String owner;
    @Option(names = {"--user-group", "--group"}, arity = "0..*")
    private ArrayList<String> userGroups;
    @Option(names = {"--microservice"}, arity = "0..*")
    private ArrayList<String> microservices;
    @Option(names = {"--release-microservice"})
    private String releaseMicroservice;

    @Mixin
    private FoDAppTypeOptions.RequiredAppTypeOption appType;
    @Mixin
    private FoDCriticalityTypeOptions.RequiredOption criticalityType;
    @Mixin
    private FoDAttributeUpdateOptions.OptionalAttrOption appAttrs;
    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredOption sdlcStatus;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validate();

        FoDUserDescriptor userDescriptor = FoDUserHelper.getUserDescriptor(unirest, owner, true);
        FoDAppTypeOptions.FoDAppType appType = this.appType.getAppType();

        FoDAppCreateRequest appCreateRequest = new FoDAppCreateRequest()
                .setApplicationName(applicationName)
                .setApplicationDescription(description)
                .setBusinessCriticalityType(String.valueOf(criticalityType.getCriticalityType()))
                .setEmailList(FoDAppHelper.getEmailList(notifications))
                .setReleaseName(releaseName)
                .setReleaseDescription(releaseDescription)
                .setSdlcStatusType(String.valueOf(sdlcStatus.getSdlcStatusType()))
                .setOwnerId(userDescriptor.getUserId())
                .setApplicationType(appType.getName())
                .setHasMicroservices(appType.isMicroservice())
                .setMicroservices(FoDAppHelper.getMicroservicesNode(microservices))
                .setReleaseMicroserviceName(releaseMicroservice)
                .setAttributes(FoDAttributeHelper.getAttributesNode(unirest, appAttrs.getAttributes()))
                .setUserGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups));

        return FoDAppHelper.createApp(unirest, appCreateRequest).asJsonNode();
    }

    private void validate() {
        if (appType.getAppType().equals(FoDAppTypeOptions.FoDAppType.Microservice)) {
            if ((FoDAppHelper.missing(microservices) || (releaseMicroservice == null || releaseMicroservice.isEmpty())))
                throw new ParameterException(spec.commandLine(),
                        "Missing option: if 'Microservice' type is specified then one or more 'microservice' options need to specified.");
            if (!microservices.contains(releaseMicroservice))
                throw new ParameterException(spec.commandLine(),
                        "Invalid option: the 'release-microservice' option specified was not found in the 'microservice' options.");
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }

}
