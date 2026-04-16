/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ssc.issue.cli.cmd;

import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueBulkEmbedMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueIncludeMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCIssueGetCommand extends AbstractSSCOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue.get.id")
    private String id;
    @Mixin private SSCIssueBulkEmbedMixin bulkEmbedMixin;
    @Mixin private SSCIssueIncludeMixin includeMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        return requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .baseRequest(getBaseRequest(unirest, appVersionId))
                .build();
    }

    private HttpRequest<?> getBaseRequest(UnirestInstance unirest, String appVersionId) {
        return unirest.get(SSCUrls.PROJECT_VERSION_ISSUE(appVersionId, id)).queryString("qm", "issues");
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
