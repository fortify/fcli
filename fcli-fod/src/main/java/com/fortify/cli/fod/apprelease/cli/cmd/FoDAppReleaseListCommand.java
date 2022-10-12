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
package com.fortify.cli.fod.apprelease.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IBaseHttpRequestSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IRecordTransformer;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.query.FoDFilterParamGenerator;
import com.fortify.cli.fod.rest.query.FoDFiltersParamValueGenerators;
import com.fortify.cli.fod.rest.query.IFoDFilterParamGeneratorSupplier;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.List.CMD_NAME)
public class FoDAppReleaseListCommand extends AbstractFoDOutputCommand implements IBaseHttpRequestSupplier, IRecordTransformer, IFoDFilterParamGeneratorSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.List outputHelper;

    @Getter private FoDFilterParamGenerator filterParamGenerator = new FoDFilterParamGenerator()
            .add("id", "releaseId", FoDFiltersParamValueGenerators::plain)
            .add("name", "releaseName", FoDFiltersParamValueGenerators::plain)
            .add("microserviceId", "microservice.id", FoDFiltersParamValueGenerators::plain)
            .add("microserviceName", "microservice.name", FoDFiltersParamValueGenerators::plain)
            .add("applicationId", "application.id", FoDFiltersParamValueGenerators::plain)
            .add("applicationName", "application.name", FoDFiltersParamValueGenerators::plain);

    @Override
    public JsonNode transformRecord(JsonNode record) {
        // TODO: change to FoDReleaseHelper when implemented
        return FoDAppHelper.renameFields(record);
    }

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.RELEASES);
    }
}
