package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@Reflectable
public class IssueData {
    private String accuracy;
    private String analyzerName;
    private String classID;
    private String confidence;
    private String defaultSeverity;
    private String impact;
    private String instanceID;
    private String instanceSeverity;
    private String filetype;
    private String kingdom;
    private String likelihood;
    private String priority;
    private String probability;
    private String subType;
    private String type;
}

