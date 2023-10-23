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
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractFoDScanListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalOption releaseResolver;
    
    @Override
    public final HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        var appId = appResolver.getAppId(unirest);
        var releaseId = releaseResolver.getReleaseId(unirest);
        HttpRequest<?> baseRequest;
        if ( appId!=null ) {
            if (releaseId!=null) {
                // specifying a release takes precedence over app as its more specific
                baseRequest = unirest.get(FoDUrls.RELEASE_SCANS).routeParam("relId", releaseId);
            } else {
                baseRequest = unirest.get(FoDUrls.APP_SCANS).routeParam("appId", appId);
            }
        } else if ( releaseId!=null ) {
            baseRequest = unirest.get(FoDUrls.RELEASE_SCANS).routeParam("relId", releaseId);
        } else {
            baseRequest = unirest.get(FoDUrls.SCANS);
        }
        return FoDScanHelper.addDefaultScanListParams(baseRequest);
    }

    @Override
    public final JsonNode transformRecord(JsonNode record) {
        var scanType = getScanType();
        return scanType!=null && !scanType.equals(record.get("scanType"))
                ? null
                : FoDScanHelper.renameFields(record);
    }
    
    protected abstract FoDScanType getScanType();

    @Override
    public final boolean isSingular() {
        return false;
    }
}
