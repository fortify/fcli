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
package com.fortify.cli.fod.issue.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueEmbedMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueIncludeMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class FoDIssueGetCommand extends AbstractFoDOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.fod.issue.get.id")
    private String vulnId;
    @Mixin private FoDIssueEmbedMixin embedMixin;
    @Mixin private FoDIssueIncludeMixin includeMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        String releaseId = releaseResolver.getReleaseId(unirest);
        JsonNode issue = getIssue(unirest, releaseId);
        return simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .source(issue)
                .build();
    }

    private JsonNode getIssue(UnirestInstance unirest, String releaseId) {
        boolean numericId = vulnId!=null && vulnId.chars().allMatch(Character::isDigit);
        JsonNode issue = numericId
            ? getIssueByFilter(unirest, releaseId, "id", vulnId)
            : getIssueByFilter(unirest, releaseId, "vulnId", vulnId);
        if ( issue==null ) {
            issue = numericId
                ? getIssueByFilter(unirest, releaseId, "vulnId", vulnId)
                : getIssueByFilter(unirest, releaseId, "id", vulnId);
        }
        if ( issue==null ) {
            throw new FcliSimpleException(String.format("No issue found for id or vulnId '%s' in the specified release", vulnId));
        }
        return issue;
    }

    private JsonNode getIssueByFilter(UnirestInstance unirest, String releaseId, String fieldName, String value) {
        HttpRequest<?> request = unirest.get(FoDUrls.VULNERABILITIES)
                .routeParam("relId", releaseId)
                .queryString("filters", fieldName+":"+value)
                .queryString("limit", "1");
        JsonNode body = includeMixin.updateRequest(request).asObject(JsonNode.class).getBody();
        JsonNode items = FoDInputTransformer.getItems(body);
        return items!=null && items.isArray() && !items.isEmpty() ? items.get(0) : null;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}