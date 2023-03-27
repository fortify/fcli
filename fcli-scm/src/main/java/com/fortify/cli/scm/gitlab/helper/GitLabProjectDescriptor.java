package com.fortify.cli.scm.gitlab.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
public class GitLabProjectDescriptor extends JsonNodeHolder {
    private static final Logger LOG = LoggerFactory.getLogger(GitLabProjectDescriptor.class);
    @Setter(onMethod_={@JsonProperty("id")}) @Getter(onMethod_={@JsonProperty("project_id")})
    private String projectId;
    @Setter(onMethod_={@JsonProperty("name")}) @Getter(onMethod_={@JsonProperty("project_name")})
    private String projectName;
    @Setter(onMethod_={@JsonProperty("path_with_namespace")}) @Getter(onMethod_={@JsonProperty("project_full_path")})
    private String projectFullPath;
    private String web_url;
    private String visibility;
    private boolean fork = false;
    
    @JsonProperty("forked_from_project")
    public void setForkedFromProject(JsonNode fork) {
        LOG.debug("Unpacking fork: "+fork);
        this.fork = true;
    }
}
