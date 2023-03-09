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

package com.fortify.cli.fod.microservice.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.microservice.cli.mixin.FoDAppAndMicroserviceNameDescriptor;
import com.fortify.cli.fod.microservice.cli.mixin.FoDAppAndMicroserviceNameResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceUpdateRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Create.CMD_NAME)
public class FoDAppMicroserviceCreateCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Update outputHelper;
    @Mixin private FoDAppAndMicroserviceNameResolverMixin.PositionalParameter appAndMicroserviceNameResolver;

    @Option(names={"--skip-if-exists"})
    private boolean skipIfExists = false;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if (skipIfExists) {
            FoDAppMicroserviceDescriptor descriptor = FoDAppMicroserviceHelper.getOptionalAppMicroserviceFromAppAndMicroserviceName(unirest, appAndMicroserviceNameResolver.getAppAndMicroserviceNameDescriptor());
            if (descriptor != null) { return descriptor.asObjectNode().put("__action__", "SKIPPED_EXISTING"); }
        }
        FoDAppAndMicroserviceNameDescriptor appAndMicroserviceNameDescriptor = FoDAppAndMicroserviceNameDescriptor.fromCombinedAppAndMicroserviceName(
                appAndMicroserviceNameResolver.getAppAndMicroserviceName(), appAndMicroserviceNameResolver.getDelimiter());

        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, appAndMicroserviceNameDescriptor.getAppName(), true);
        FoDAppMicroserviceUpdateRequest msCreateRequest = new FoDAppMicroserviceUpdateRequest()
                .setMicroserviceName(appAndMicroserviceNameDescriptor.getMicroserviceName());
        return FoDAppMicroserviceHelper.createAppMicroservice(unirest, appDescriptor.getApplicationId(), msCreateRequest);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppMicroserviceHelper.renameFields(record);
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
