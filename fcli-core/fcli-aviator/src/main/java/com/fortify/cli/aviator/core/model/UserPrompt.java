package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

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
