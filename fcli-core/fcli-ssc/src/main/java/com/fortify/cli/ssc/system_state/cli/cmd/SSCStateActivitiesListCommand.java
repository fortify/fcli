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
package com.fortify.cli.ssc.system_state.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.query.cli.mixin.SSCQParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-activities", aliases = {"lsa"}) @CommandGroup("activity")
public class SSCStateActivitiesListCommand extends AbstractSSCBaseRequestOutputCommand implements IRecordTransformer, IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper; 
    @Mixin private SSCQParamMixin qParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
            .add("userName", SSCQParamValueGenerators::wrapInQuotes)
            .add("eventType", SSCQParamValueGenerators::wrapInQuotes)
            .add("detailedNote", SSCQParamValueGenerators::wrapInQuotes)
            .add("applicationVersionId", "projectVersionId", SSCQParamValueGenerators::plain);
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.ACTIVITY_FEED_EVENTS);
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return new RenameFieldsTransformer("projectVersionId", "applicationVersionId").transform(record);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
