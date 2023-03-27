package com.fortify.cli.scm.github.cli.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.progress.cli.mixin.ProgressHelperMixin;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.scm.github.cli.util.GitHubPagingHelper;
import com.fortify.cli.scm.github.helper.GitHubRepoSlugDescriptor;
import com.fortify.cli.scm.github.helper.GitHubRepoSlugTypeConverter;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public abstract class AbstractGitHubRepoProcessorMixin {
    @ArgGroup(exclusive = true)
    private GitHubRepoSelectorOptions repoSelectorOptions = new GitHubRepoSelectorOptions();
    
    public void process(UnirestInstance unirest, ProgressHelperMixin progressHelper) {
        if ( StringUtils.isNotBlank(repoSelectorOptions.search) ) {
            HttpRequest<?> req = unirest.get("/search/repositories")
                    .queryString("q", repoSelectorOptions.search);
            GitHubPagingHelper.pagedRequest(req, ObjectNode.class)
                .ifSuccess(r->r.getBody().get("items").forEach(repo->processRepo(unirest, progressHelper, repo)));
        } else if ( !repoSelectorOptions.repoSlugs.isEmpty() ) {
            repoSelectorOptions.repoSlugs.forEach(repoSlug->processRepoSlug(unirest, progressHelper, repoSlug));
        } else if ( !repoSelectorOptions.organizations.isEmpty() ) {
            repoSelectorOptions.organizations.forEach(orgName->processOrg(unirest, progressHelper, orgName));
        } else {
            progressHelper.writeI18nProgress("loading.organizations");
            HttpRequest<?> req = unirest.get("/user/orgs");
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(org->processOrg(unirest, progressHelper, org.get("login").asText())));
        }
    }
    
    protected abstract void processRepo(UnirestInstance unirest, ProgressHelperMixin progressHelper, JsonNode repoNode);
    
    private final void processOrg(UnirestInstance unirest, ProgressHelperMixin progressHelper, String orgName) {
        progressHelper.writeI18nProgress("loading.repositories", orgName);
        HttpRequest<?> req = unirest.get("/orgs/{org}/repos?type=all")
                .routeParam("org", orgName);
        GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
            .ifSuccess(r->r.getBody().forEach(repo->processRepo(unirest, progressHelper, repo)));
    }
    
    private void processRepoSlug(UnirestInstance unirest, ProgressHelperMixin progressHelper, GitHubRepoSlugDescriptor repoSlug) {
        unirest.get("/repos/{owner}/{repo}")
            .routeParam("owner", repoSlug.getOwnerName())
            .routeParam("repo", repoSlug.getRepoName())
            .asObject(JsonNode.class)
            .ifSuccess(r->processRepo(unirest, progressHelper, r.getBody()));
    }
    
    private static final class GitHubRepoSelectorOptions {
        @Option(names = {"--organizations", "--orgs"}, split=",")
        private List<String> organizations = new ArrayList<>();
        
        @Option(names = {"--repositories", "--repos"}, split=",", converter = GitHubRepoSlugTypeConverter.class)
        private Set<GitHubRepoSlugDescriptor> repoSlugs = new HashSet<>();
        
        @Option(names = "--search")
        private String search;
    }
}