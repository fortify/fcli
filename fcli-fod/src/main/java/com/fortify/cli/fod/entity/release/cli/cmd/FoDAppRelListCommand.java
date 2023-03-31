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

package com.fortify.cli.fod.entity.release.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.query.FoDFiltersParamGenerator;
import com.fortify.cli.fod.rest.query.cli.mixin.FoDFiltersParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDAppRelListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer, IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDFiltersParamMixin filterParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new FoDFiltersParamGenerator()
            .add("releaseId")
            .add("releaseName")
            .add("microserviceId")
            .add("microserviceName")
            .add("applicationId")
            .add("applicationName");

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppRelHelper.renameFields(record);
    }

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        if (appResolver != null && appResolver.getAppNameOrId() != null)
            return unirest.get(FoDUrls.APPLICATION_RELEASES).routeParam("appId", appResolver.getAppId(unirest));
        else
            return unirest.get(FoDUrls.RELEASES);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
