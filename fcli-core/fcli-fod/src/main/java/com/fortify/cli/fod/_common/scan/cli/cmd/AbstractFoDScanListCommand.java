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
package com.fortify.cli.fod._common.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@CommandGroup("*-scan")
public abstract class AbstractFoDScanListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    
    @Override
    public final HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        var releaseId = releaseResolver.getReleaseId(unirest);
        var baseRequest = unirest.get(FoDUrls.RELEASE_SCANS).routeParam("relId", releaseId);
        return FoDScanHelper.addDefaultScanListParams(baseRequest);
    }

    @Override
    public final JsonNode transformRecord(JsonNode record) {
        var scanType = getScanType();
        return scanType!=null && !scanType.name().equals(record.get("scanType").asText())
                ? null
                : record;
    }
    
    protected abstract FoDScanType getScanType();

    @Override
    public final boolean isSingular() {
        return false;
    }
}
