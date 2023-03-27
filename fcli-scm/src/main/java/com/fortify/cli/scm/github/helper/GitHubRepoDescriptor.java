package com.fortify.cli.scm.github.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
public class GitHubRepoDescriptor extends JsonNodeHolder {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepoDescriptor.class);
    @Getter(onMethod_=@JsonProperty("owner_name"))
    private String ownerName;
    @Setter(onMethod_=@JsonProperty("name")) @Getter(onMethod_=@JsonProperty("repo_name"))
    private String repoName;
    @JsonProperty("full_name")
    private String fullName;
    private String html_url;
    private String visibility;
    private boolean fork;
    
    @JsonProperty("owner")
    public void setOwner(ObjectNode owner) {
        LOG.debug("Unpacking owner: "+owner);
        this.ownerName = owner.get("login").asText();
    }
}
