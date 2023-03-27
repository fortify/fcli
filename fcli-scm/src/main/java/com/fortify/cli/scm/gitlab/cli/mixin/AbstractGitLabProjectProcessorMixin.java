package com.fortify.cli.scm.gitlab.cli.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.progress.cli.mixin.ProgressHelperMixin;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.scm.github.cli.util.GitHubPagingHelper;
import com.fortify.cli.scm.gitlab.cli.util.GitLabPagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public abstract class AbstractGitLabProjectProcessorMixin {
    @ArgGroup(exclusive = true)
    private GitLabProjectSelectorOptions projectSelectorOptions = new GitLabProjectSelectorOptions();
    
    public void process(UnirestInstance unirest, ProgressHelperMixin progressHelper) {
        if ( StringUtils.isNotBlank(projectSelectorOptions.search) ) {
            HttpRequest<?> req = unirest.get("/api/v4/search")
                    .queryString("scope", "projects")
                    .queryString("search", projectSelectorOptions.search);
            // Search API doesn't return full project details, so we call processProject with 'id' attribute to load full details
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(project->processProject(unirest, progressHelper, project.get("id").asText())));
        } else if ( StringUtils.isNotBlank(projectSelectorOptions.topic) ) {
            HttpRequest<?> req = unirest.get("/api/v4/projects")
                    .queryString("topic", projectSelectorOptions.topic);
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(project->processProject(unirest, progressHelper, project))); 
        } else if ( !projectSelectorOptions.groupSelectorOptions.groups.isEmpty() ) {
            projectSelectorOptions.groupSelectorOptions.groups
                .forEach(group->processGroup(unirest, progressHelper, group));
        } else if ( !projectSelectorOptions.projects.isEmpty() ) {
            projectSelectorOptions.projects.forEach(projectName->processProject(unirest, progressHelper, projectName));
        } else {
            progressHelper.writeI18nProgress("loading.groups");
            HttpRequest<?> req = unirest.get("/api/v4/groups").queryString("owned", "true");
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(group->processGroup(unirest, progressHelper, group.get("full_path").asText())));
        }
    }
    
    protected abstract void processProject(UnirestInstance unirest, ProgressHelperMixin progressHelper, JsonNode projectNode);
    
    private final void processGroup(UnirestInstance unirest, ProgressHelperMixin progressHelper, String groupPath) {
        progressHelper.writeI18nProgress("loading.projects", groupPath);
        HttpRequest<?> req = unirest.get("/api/v4/groups/{path}/projects")
                .routeParam("path", groupPath)
                .queryString("include_subgroups", projectSelectorOptions.groupSelectorOptions.includeSubGroups);
        GitLabPagingHelper.pagedRequest(req, ArrayNode.class)
            .ifSuccess(r->r.getBody().forEach(project->processProject(unirest, progressHelper, project)));
    }
    
    
    private void processProject(UnirestInstance unirest, ProgressHelperMixin progressHelper, String projectPath) {
        unirest.get("/api/v4/projects/{path}")
            .routeParam("path", projectPath)
            .asObject(JsonNode.class)
            .ifSuccess(r->processProject(unirest, progressHelper, r.getBody()));
    }
    
    private static final class GitLabProjectSelectorOptions {
        @ArgGroup(exclusive=false, multiplicity = "0..1")
        private GitLabGroupSelectorOptions groupSelectorOptions = new GitLabGroupSelectorOptions();
        
        @Option(names = {"--projects"}, split=",", required=false)
        private Set<String> projects = new HashSet<>();
        
        @Option(names = "--search")
        private String search;
        
        @Option(names = "--topic")
        private String topic;
    }
    
    private static final class GitLabGroupSelectorOptions {
        @Option(names = {"--groups"}, split=",", required=true)
        private List<String> groups = new ArrayList<>();
        
        @Option(names = {"--no-subgroups"}, negatable=true)
        private boolean includeSubGroups = true;
    }
}