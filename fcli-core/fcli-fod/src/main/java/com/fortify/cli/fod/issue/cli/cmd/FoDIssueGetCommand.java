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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueEmbedMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class FoDIssueGetCommand extends AbstractFoDOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.fod.issue.get.vulnId")
    private String vulnId;
    @Mixin private FoDIssueEmbedMixin embedMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        ObjectNode issue = findIssue(unirest, releaseDescriptor);
        issue.put("releaseName", releaseDescriptor.getReleaseName());
        return simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .source(issue)
                .build();
    }

    private ObjectNode findIssue(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        String releaseId = releaseDescriptor.getReleaseId().toString();
        HttpRequest<?> request = unirest.get(FoDUrls.VULNERABILITIES)
                .routeParam("relId", releaseId)
                .queryString("filters", "vulnId:" + vulnId)
                .queryString("includeFixed", "true")
                .queryString("includeSuppressed", "true")
                .queryString("limit", "2");
        JsonNode body = request.asObject(JsonNode.class).getBody();
        JsonNode items = FoDInputTransformer.getItems(body);
        if ( items==null || !items.isArray() || items.isEmpty() ) {
            throw new FcliSimpleException(String.format("No vulnerability found for vulnId '%s' in release '%s'", vulnId, releaseDescriptor.getReleaseName()));
        }
        if ( items.size()>1 ) {
            throw new FcliSimpleException(String.format("Multiple vulnerabilities found for vulnId '%s'; please check your input", vulnId));
        }
        JsonNode issue = items.get(0);
        if ( !(issue instanceof ObjectNode issueObject) ) {
            throw new FcliTechnicalException(String.format("Unexpected response for vulnId '%s'; expected a JSON object", vulnId));
        }
        return issueObject;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}