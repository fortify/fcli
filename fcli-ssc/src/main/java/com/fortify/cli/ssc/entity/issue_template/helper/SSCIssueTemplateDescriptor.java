package com.fortify.cli.ssc.entity.issue_template.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
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
