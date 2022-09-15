package com.fortify.cli.ssc.issue_template.helper;

import com.fortify.cli.common.util.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class SSCIssueTemplateDescriptor extends JsonNodeHolder {
    private String id;
    private String name;
    private String description;
    private boolean inUse;
    private boolean defaultTemplate;
    private String publishVersion;
    private String originalFileName;
}
