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
package com.fortify.cli.aviator.audit;

import java.util.Set;

import com.fortify.cli.aviator.audit.model.AnalysisInfo;
import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.util.StringUtil;

public class IssueObjBuilder {

    public static UserPrompt buildIssueObj(Vulnerability vulnerability, SourceLanguageResolver languageResolver) {

        IssueData issueData = IssueData.builder()
                .accuracy(String.valueOf(vulnerability.getAccuracy()))
                .analyzerName(vulnerability.getAnalyzerName())
                .classID(vulnerability.getClassID())
                .defaultSeverity(String.valueOf(vulnerability.getDefaultSeverity()))
                .impact(String.valueOf(vulnerability.getImpact()))
                .instanceID(vulnerability.getInstanceID())
                .instanceSeverity(String.valueOf(vulnerability.getInstanceSeverity()))
                .filetype(vulnerability.getFiletype())
                .kingdom(vulnerability.getKingdom())
                .likelihood(vulnerability.getLikelihood())
                .priority(vulnerability.getPriority())
                .probability(String.valueOf(vulnerability.getProbability()))
                .confidence(String.valueOf(vulnerability.getConfidence()))
                .subType(vulnerability.getSubType())
                .type(vulnerability.getType())
                .build();

        AnalysisInfo analysisInfo = AnalysisInfo.builder()
                .shortDescription(vulnerability.getShortDescription())
                .explanation(vulnerability.getExplanation())
                .build();

        Set<String> programmingLanguages = languageResolver.resolveProgrammingLanguages(vulnerability);
        String language = languageResolver.resolvePrimaryLanguage(vulnerability);
        String fileExtension = languageResolver.resolvePrimaryFileExtension(vulnerability);

        return UserPrompt.builder()
                .issueData(issueData)
                .analysisInfo(analysisInfo)
                .stackTrace(vulnerability.getStackTrace())
                .firstStackTrace(vulnerability.getFirstStackTrace())
                .longestStackTrace(vulnerability.getLongestStackTrace())
                .files(vulnerability.getFiles())
                .lastStackTraceElement(vulnerability.getLastStackTraceElement())
                .programmingLanguages(programmingLanguages)
                .fileExtension(StringUtil.isEmpty(fileExtension) ? "Unknown" : fileExtension)
                .language(StringUtil.isEmpty(language) ? "Unknown" : language)
                .category(StringUtil.isEmpty(vulnerability.getCategory()) ? "Unknown" : vulnerability.getCategory())
                .tier("")
                .source(vulnerability.getSource())
                .sink(vulnerability.getSink())
                .build();
    }
}