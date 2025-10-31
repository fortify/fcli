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
package com.fortify.cli.aviator.audit.model;

import java.util.List;
import java.util.Set;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Reflectable
public class UserPrompt {
    private IssueData issueData;
    private AnalysisInfo analysisInfo;
    private List<List<StackTraceElement>> stackTrace;
    private List<StackTraceElement> firstStackTrace;
    private List<StackTraceElement> longestStackTrace;
    private List<File> files;
    private StackTraceElement lastStackTraceElement;
    private Set<String> programmingLanguages;
    private String fileExtension;
    private String language;
    private String category;
    private String tier;
    private StackTraceElement source;
    private StackTraceElement sink;
    private String categoryLevel;
}
