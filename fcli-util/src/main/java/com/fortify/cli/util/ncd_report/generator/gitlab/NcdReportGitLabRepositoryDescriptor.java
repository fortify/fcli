package com.fortify.cli.util.ncd_report.generator.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

/**
 * GitLab-specific implementation of {@link INcdReportRepositoryDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitLabRepositoryDescriptor extends JsonNodeHolder implements INcdReportRepositoryDescriptor {
    @JsonProperty("id")
    private String id;
    @JsonProperty("path_with_namespace")
    private String fullName;
    @JsonProperty("web_url")
    private String url;
    private String namespaceFullPath;
    @Setter(onMethod_=@JsonProperty("path"))
    private String name;
    private String visibility;
    private boolean fork = false; // TODO
    @JsonProperty("empty_repo")
    private boolean empty;
    private String branchesUrl;
    
    @JsonProperty("namespace")
    public void setNameSpace(ObjectNode namespace) {
        this.namespaceFullPath = namespace.get("full_path").asText();
    }
    
    @JsonProperty("forked_from_project")
    public void setForkedFromProject(ObjectNode forkedFrom) {
        if ( !forkedFrom.isEmpty() ) {
            this.fork = true;
        }
    }
    
    @JsonProperty("_links")
    public void setLinks(ObjectNode links) {
        this.branchesUrl = links.get("repo_branches").asText();
    }
}
