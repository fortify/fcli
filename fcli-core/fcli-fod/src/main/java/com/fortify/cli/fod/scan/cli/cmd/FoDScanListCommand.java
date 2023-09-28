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
package com.fortify.cli.fod.scan.cli.cmd;

import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDAnalysisStatusType;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME, hidden = false)
public class FoDScanListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;

    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalOption releaseResolver;
    @Getter @Option(names = {"--type"}, required = false, descriptionKey = "fcli.fod.scan.list.type") FoDScanType type;
    @Getter @Option(names = {"--status"}, required = false, descriptionKey = "fcli.fod.scan.list.status") FoDAnalysisStatusType status;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
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
        return updateRequest(baseRequest);
    }

    private HttpRequest<?> updateRequest(HttpRequest<?> request) {
        request.queryString("orderBy", "startedDateTime");
        request.queryString("orderByDirection", "DESC");
        return request;
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return getFilterPredicate().test(record)
                ? FoDScanHelper.renameFields(record)
                : null;
    }

    @Override
    public boolean isSingular() {
        return false;
    }

    protected Predicate<JsonNode> getFilterPredicate() {
        Predicate<JsonNode> result = o->true;
        result = and(result, "scanType", type);
        result = and(result, "analysisStatusType", status);
        return result;
    }

    protected final Predicate<JsonNode> and(Predicate<JsonNode> p, String name, Enum<?> value) {
        return value==null ? p : p.and(o->o.get(name).asText().equals(value.name()));
    }

}
