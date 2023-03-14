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

package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.helper.FoDFilterResultsTransformer;
import com.fortify.cli.fod.rest.query.FoDFilterParamGenerator;
import com.fortify.cli.fod.rest.query.FoDFiltersParamValueGenerators;
import com.fortify.cli.fod.rest.query.IFoDFilterParamGeneratorSupplier;
import com.fortify.cli.fod.scan.cli.mixin.FoDAnalysisStatusTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.List.CMD_NAME)
public class FoDSastScanListCommand extends AbstractFoDOutputCommand implements IUnirestBaseRequestSupplier, IRecordTransformer, IFoDFilterParamGeneratorSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.List outputHelper;

    @Getter private FoDFilterParamGenerator filterParamGenerator = new FoDFilterParamGenerator()
            .add("id","scanId", FoDFiltersParamValueGenerators::plain)
            .add("type", "scanType", FoDFiltersParamValueGenerators::plain);

    @Mixin private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;

    // TODO Consider standardizing sorting options across fcli modules, also see https://github.com/fortify/fcli/issues/86
    @Option(names = {"--latest-first"})
    private Boolean latestFirst;

    @Mixin private FoDAnalysisStatusTypeOptions.OptionalOption analysisStatus;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return updateRequest(
                unirest.get(FoDUrls.RELEASE + "/scans")
                        .routeParam("relId", appMicroserviceRelResolver.getAppMicroserviceRelId(unirest))
        );
    }

    private HttpRequest<?> updateRequest(HttpRequest<?> request) {
        request.queryString("orderByDirection", (latestFirst != null && latestFirst ? "DESC" : "ASC"));
        return request;
    }
    @Override
    public JsonNode transformRecord(JsonNode record) {
        String aStatusStr = (analysisStatus != null && analysisStatus.getAnalysisStatusType() != null? String.valueOf(analysisStatus.getAnalysisStatusType()) : "*");
        return new FoDFilterResultsTransformer(new String[] {
                "scanType:Static", "analysisStatusType:"+aStatusStr
        }).transform(FoDScanHelper.renameFields(record));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
