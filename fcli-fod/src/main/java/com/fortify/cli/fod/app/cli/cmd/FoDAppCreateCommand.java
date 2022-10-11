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

import com.fortify.cli.fod.app.helper.FoDAppCreateRequest;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.app.mixin.FoDAppTypeOptions;
import com.fortify.cli.fod.app.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.app.mixin.FoDSdlcStatusTypeOptions;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDUpdateCommand;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ArrayList;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppCreateCommand extends AbstractFoDUpdateCommand {
    @Spec CommandSpec spec;

    @Parameters(index = "0", arity = "1", descriptionKey = "appName")
    private String applicationName;
    @Parameters(index = "1", arity = "1", descriptionKey = "relName")
    private String releaseName;
    @Option(names = {"--description", "-d"}, descriptionKey = "appDesc")
    private String description;
    @Option(names = {"--notify"}, required = false, arity = "0..*", descriptionKey = "notify")
    private ArrayList<String> notifications;
    @Option(names = {"--release-description", "--rel-desc"}, descriptionKey = "relDesc")
    private String releaseDescription;
    @Option(names = {"--owner"}, required = true, descriptionKey = "owner")
    private String owner;
    @Option(names = {"--user-group", "--group"}, arity = "0..*", descriptionKey = "userGroup")
    private ArrayList<String> userGroups;
    @Option(names = {"--microservice"}, arity = "0..*", descriptionKey = "microservice")
    private ArrayList<String> microservices;
    @Option(names = {"--release-microservice"}, descriptionKey = "releaseMs")
    private String releaseMicroservice;

    @Mixin
    private FoDAppTypeOptions.RequiredAppTypeOption appType;
    @Mixin
    private FoDCriticalityTypeOptions.RequiredCritOption criticalityType;
    @Mixin
    private FoDAttributeUpdateOptions.OptionalAttrOption appAttrs;
    @Mixin
    private FoDSdlcStatusTypeOptions.RequiredSdlcOption sdlcStatus;

    @SneakyThrows
    @Override
    protected Void run(UnirestInstance unirest) {
        validate();

        FoDUserDescriptor userDescriptor = FoDUserHelper.getUser(unirest, owner, true);
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
                .setUserGroupIds(FoDAppHelper.getUserGroupsNode(unirest, userGroups));

        FoDAppDescriptor result = FoDAppHelper.createApp(unirest, appCreateRequest);
        getOutputMixin().write(result.asObjectNode());
        return null;
    }

    private void validate() {
        if (appType.getAppType().equals(FoDAppTypeOptions.FoDAppType.Microservice)) {
            if ((missing(microservices) || (releaseMicroservice == null || releaseMicroservice.isEmpty())))
                throw new ParameterException(spec.commandLine(),
                        "Missing option: if 'Microservice' type is specified then " +
                                "one or more '-microservice' names need to specified " +
                                "as well as the microservice to create the release for " +
                                "using '--release-microservice");
            if (!microservices.contains(releaseMicroservice))
                throw new ParameterException(spec.commandLine(),
                        "Invalid option: the '--release-microservice' specified was not " +
                                "included in the 'microservice' options");
        }
    }

}
