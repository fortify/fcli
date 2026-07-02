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

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueBulkEmbedMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCIssueGetCommand extends AbstractSSCOutputCommand implements IHttpRequestUpdater {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @EnvSuffix("ISSUE_ID") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.issue.get.id")
    private String id;
    @Mixin private SSCIssueBulkEmbedMixin bulkEmbedMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        return requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .baseRequest(getBaseRequest(unirest, appVersionId))
                .build();
    }

    private HttpRequest<?> getBaseRequest(UnirestInstance unirest, String appVersionId) {
        return unirest.get(SSCUrls.PROJECT_VERSION_ISSUE(appVersionId, id))
                .queryString("showHidden", "true")
                .queryString("showRemoved", "true")
                .queryString("showSuppressed", "true");
    }

    @Override
    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        var embedSuppliers = bulkEmbedMixin.getEmbedSuppliers();
        if (embedSuppliers == null || embedSuppliers.length == 0) {
            return request.queryString("qm", "issues");
        }
        var embedNames = Arrays.stream(embedSuppliers).map(Enum::name).collect(Collectors.joining(","));
        return request.queryString("qm", "issues," + embedNames);
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
