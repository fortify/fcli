package com.fortify.cli.scm.gitlab.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
@JsonPropertyOrder({"project_id","full_path","project_name", "visibility"})
public class GitLabProjectDescriptor extends JsonNodeHolder {
    @Setter(onMethod_={@JsonProperty("id")}) @Getter(onMethod_={@JsonProperty("project_id")})
    private String projectId;
    @Setter(onMethod_={@JsonProperty("name")}) @Getter(onMethod_={@JsonProperty("project_name")})
    private String projectName;
    @Setter(onMethod_={@JsonProperty("path_with_namespace")}) @Getter(onMethod_={@JsonProperty("project_full_path")})
    private String projectFullPath;
    private String visibility;
}
