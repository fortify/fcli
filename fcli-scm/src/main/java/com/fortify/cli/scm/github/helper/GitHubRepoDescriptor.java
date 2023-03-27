package com.fortify.cli.scm.github.helper;

import java.util.Map;

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
@JsonPropertyOrder({"owner_name","repo_name","full_name","visibility","fork"})
public class GitHubRepoDescriptor extends JsonNodeHolder {
    @JsonProperty("owner_name")
    private String ownerName;
    @Setter(onMethod_=@JsonProperty("name")) @Getter(onMethod_=@JsonProperty("repo_name"))
    private String repoName;
    @JsonProperty("full_name")
    private String fullName;
    private String visibility;
    private boolean fork;
    
    @JsonProperty("owner")
    private void unpackOwner(Map<String, String> owner) {
        ownerName = owner.get("login");
    }
}
