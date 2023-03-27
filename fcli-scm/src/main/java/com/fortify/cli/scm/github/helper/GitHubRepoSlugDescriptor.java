package com.fortify.cli.scm.github.helper;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public final class GitHubRepoSlugDescriptor {
    private String ownerName;
    private String repoName;
    public String getFullName() {
        return String.format("%s/%s", ownerName, repoName);
    }
}