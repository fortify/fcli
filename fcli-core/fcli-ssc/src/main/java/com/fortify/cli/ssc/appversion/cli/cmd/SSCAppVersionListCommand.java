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
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.query.cli.mixin.SSCQParamMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionBulkEmbedMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionIncludeMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCAppVersionListCommand extends AbstractSSCBaseRequestOutputCommand implements IRecordTransformer, IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCQParamMixin qParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
                .add("id", SSCQParamValueGenerators::plain)
                .add("application.name", "project.name", SSCQParamValueGenerators::wrapInQuotes)
                .add("application.id", "project.id", SSCQParamValueGenerators::plain)
                .add("name", SSCQParamValueGenerators::wrapInQuotes);
    @Mixin private SSCAppVersionBulkEmbedMixin bulkEmbedMixin;
    @Mixin private SSCAppVersionIncludeMixin includeMixin;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/projectVersions?limit=100");
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
