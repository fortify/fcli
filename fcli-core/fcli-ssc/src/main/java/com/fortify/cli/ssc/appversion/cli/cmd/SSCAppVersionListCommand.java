/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.ssc.appversion.cli.cmd;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.RequestObjectNodeProducer.RequestObjectNodeProducerBuilder;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.ssc.query.cli.mixin.SSCQParamMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionBulkEmbedMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionExcludeMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionIncludeMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCAppVersionListCommand extends AbstractSSCOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Mixin CommandHelperMixin cmdHelper;
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCQParamMixin qParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
                .add("id", SSCQParamValueGenerators::plain)
                .add("application.name", "project.name", SSCQParamValueGenerators::wrapInQuotes)
                .add("application.id", "project.id", SSCQParamValueGenerators::plain)
                .add("name", SSCQParamValueGenerators::wrapInQuotes);
    @Mixin private SSCAppVersionBulkEmbedMixin bulkEmbedMixin;
    @Mixin private SSCAppVersionIncludeMixin includeMixin;
    @Mixin private SSCAppVersionExcludeMixin excludeMixin;
    
    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        var unirest = getUnirestInstance();
        return RequestObjectNodeProducerBuilder.builder()
                .initialRequest(unirest.get("/api/v1/projectVersions?limit=100"))
                .productHelper(getProductHelper())
                .applyFromSpec(cmdHelper.getCommandSpec())
                .recordTransformer(SSCAppVersionHelper::renameFields)
                .build();
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
