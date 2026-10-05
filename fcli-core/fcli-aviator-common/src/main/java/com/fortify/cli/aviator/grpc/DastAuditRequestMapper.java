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
package com.fortify.cli.aviator.grpc;

import com.fortify.aviator.dastaudit.DastClassification;
import com.fortify.aviator.dastaudit.DastFindingContext;
import com.fortify.aviator.dastaudit.DastReproStep;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.DastSession;

/**
 * Maps parsed WebInspect data to the DAST audit wire contract.
 */
public final class DastAuditRequestMapper {
    private DastAuditRequestMapper() {}

    public static DastFindingContext toFindingContext(DastSession session, DastIssue issue) {
        var builder = DastFindingContext.newBuilder()
            .setIssueId(value(issue.getId()))
            .setCheckTypeId(value(issue.getCheckTypeId()))
            .setEngineType(value(issue.getEngineType()))
            .setVulnerabilityId(value(issue.getVulnerabilityId()))
            .setSeverity(issue.getSeverity())
            .setName(value(issue.getName()))
            .setCategory(value(issue.getCategory()))
            .setCweId(value(issue.getCweId()))
            .setCweDescription(value(issue.getCweDescription()))
            .setSessionUrl(value(session.getUrl() != null ? session.getUrl() : issue.getSessionUrl()))
            .setSummary(value(issue.getSummary()))
            .setImplication(value(issue.getImplication()))
            .setExecution(value(issue.getExecution()))
            .setFix(value(issue.getFix()))
            .setReferenceInfo(value(issue.getReferenceInfo()))
            .setRequestId(value(session.getRequestId()))
            .setScheme(value(session.getScheme()))
            .setHost(value(session.getHost()))
            .setPort(session.getPort())
            .setAttackParamDescriptor(value(session.getAttackParamDescriptor()))
            .setRawRequest(value(session.getRawRequest()))
            .setRawResponse(value(session.getRawResponse()));

        issue.getClassifications().forEach((kind, classificationValue) -> builder.addClassifications(
            DastClassification.newBuilder().setKind(value(kind)).setValue(value(classificationValue)).build()));
        builder.addAllReproStepUrls(issue.getReproStepUrls());
        issue.getReproSteps().forEach(step -> builder.addReproSteps(DastReproStep.newBuilder()
            .setSource(value(step.getSource()))
            .setUrl(value(step.getUrl()))
            .setPostParams(value(step.getPostParams()))
            .build()));
        return builder.build();
    }

    private static String value(String value) {
        return value != null ? value : "";
    }
}