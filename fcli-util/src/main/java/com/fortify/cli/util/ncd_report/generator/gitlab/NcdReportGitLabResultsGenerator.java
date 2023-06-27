/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.ncd_report.generator.gitlab;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.util.ncd_report.collector.INcdReportRepositoryProcessor;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.util.ncd_report.config.NcdReportCombinedRepoSelectorConfig;
import com.fortify.cli.util.ncd_report.config.NcdReportGitLabGroupConfig;
import com.fortify.cli.util.ncd_report.config.NcdReportGitLabSourceConfig;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.util.ncd_report.generator.AbstractNcdReportUnirestResultsGenerator;
import com.fortify.cli.util.ncd_report.generator.github.GitHubPagingHelper;

import io.micrometer.common.util.StringUtils;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

/**
 * This class is responsible for loading repository, branch, commit and author
 * data from GitLab.
 * 
 * @author rsenden
 *
 */
public class NcdReportGitLabResultsGenerator extends AbstractNcdReportUnirestResultsGenerator<NcdReportGitLabSourceConfig> {
    /**
     * Constructor to configure this instance with the given 
     * {@link NcdReportGitLabSourceConfig} and
     * {@link NcdReportResultsCollector}.
     */
    public NcdReportGitLabResultsGenerator(NcdReportGitLabSourceConfig sourceConfig, NcdReportResultsCollector resultsCollector) {        
        super(sourceConfig, resultsCollector);
    }

    /**
     * Primary method for generating report results. This gets the 
     * group configurations, and for each group, calls 
     * the {@link #generateResults(NcdReportGitLabGroupConfig)}
     * method to load the repositories for that group and optionally
     * sub-groups.
     */
    @Override
    protected void generateResults() {
        Stream.of(sourceConfig().getGroups()).forEach(this::generateResults);
    }
    
    /**
     * This method loads the repositories for the group specified in the
     * given {@link NcdReportGitLabGroupConfig}, optionally including 
     * repositories from sub-groups as well, and passes the descriptor
     * for each repository to the {@link INcdReportRepositoryProcessor} provided 
     * by our {@link NcdReportResultsCollector}. The {@link INcdReportRepositoryProcessor}
     * will in turn call our {@link #generateCommitData(NcdReportGitLabRepositoryDescriptor, INcdReportRepositoryBranchCommitCollector)}
     * method to generate commit data for every repository that is not excluded from
     * the report.
     */
    private void generateResults(NcdReportGitLabGroupConfig groupConfig) {
        String groupId = groupConfig.getId();
        try {
            boolean includeSubgroups = groupConfig.getIncludeSubgroups().orElse(sourceConfig().getIncludeSubgroups().orElse(true));
            resultsCollector().progressWriter().writeI18nProgress("fcli.util.ncd-report.loading.gitlab-repositories", groupId);
            HttpRequest<?> req = unirest().get("/api/v4/groups/{id}/projects?per_page=100")
                    .routeParam("id", groupId)
                    .queryString("include_subgroups", includeSubgroups);
            GitLabPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(project->
                    resultsCollector().repositoryProcessor().processRepository(new NcdReportCombinedRepoSelectorConfig(sourceConfig(), groupConfig), getRepoDescriptor(project), this::generateCommitData)));
        } catch ( Exception e ) {
            resultsCollector().logger().error(String.format("Error processing group: %s (%s)", groupId, sourceConfig().getBaseUrl()), e);
        }
    }
    
    /**
     * This method generates commit data for the given repository by retrieving
     * all branches, and then invoking the {@link #generateCommitDataForBranches(INcdReportRepositoryBranchCommitCollector, NcdReportGitLabRepositoryDescriptor, List)}
     * method to generate commit data for each branch. If no commits are found that
     * match the date range, the {@link #generateMostRecentCommitData(INcdReportRepositoryBranchCommitCollector, NcdReportGitLabRepositoryDescriptor, List)}
     * method is invoked to find the most recent commit older than the date range.
     */
    private void generateCommitData(NcdReportGitLabRepositoryDescriptor repoDescriptor, INcdReportRepositoryBranchCommitCollector branchCommitCollector) {
        var branchDescriptors = getBranchDescriptors(repoDescriptor);
        boolean commitsFound = generateCommitDataForBranches(branchCommitCollector, repoDescriptor, branchDescriptors);
        if ( !commitsFound ) {
            generateMostRecentCommitData(branchCommitCollector, repoDescriptor, branchDescriptors);
        }
    }

    /**
     * This method loads the latest commit for every branch, then passes the overall
     * latest commit (if found) to the {@link #addCommit(INcdReportRepositoryBranchCommitCollector, NcdReportGitLabRepositoryDescriptor, NcdReportGitLabBranchDescriptor, JsonNode)}
     * method.
     */
    private void generateMostRecentCommitData(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportGitLabRepositoryDescriptor repoDescriptor, List<NcdReportGitLabBranchDescriptor> branchDescriptors) {
        NcdReportGitLabCommitDescriptor mostRecentCommitDescriptor = null;
        NcdReportGitLabBranchDescriptor mostRecentBranchDescriptor = null;
        for ( var branchDescriptor : branchDescriptors ) {
            var currentCommitResponse = getCommitsRequest(repoDescriptor, branchDescriptor, 1)
                .asObject(ArrayNode.class).getBody();
            if ( currentCommitResponse.size()>0 ) {
                var currentCommitDescriptor = JsonHelper.treeToValue(currentCommitResponse.get(0), NcdReportGitLabCommitDescriptor.class);
                if ( mostRecentCommitDescriptor==null || currentCommitDescriptor.getDate().isAfter(mostRecentCommitDescriptor.getDate()) ) {
                    mostRecentCommitDescriptor = currentCommitDescriptor;
                    mostRecentBranchDescriptor = branchDescriptor;
                }
            }
        }
        if ( mostRecentCommitDescriptor!=null ) {
            addCommit(branchCommitCollector, repoDescriptor, mostRecentBranchDescriptor, mostRecentCommitDescriptor.asJsonNode());
        }
    }

    /**
     * This method generates commit data for all commits later than the configured
     * date/time for all branches.
     * @return true if any commits were found, false otherwise  
     */
    private boolean generateCommitDataForBranches(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportGitLabRepositoryDescriptor repoDescriptor, List<NcdReportGitLabBranchDescriptor> branchDescriptors) {
        String since = resultsCollector().reportConfig().getCommitOffsetDateTime()
                .format(DateTimeFormatter.ISO_INSTANT);
        boolean commitsFound = false;
        for ( var branchDescriptor : branchDescriptors ) {
            resultsCollector().progressWriter().writeI18nProgress("fcli.util.ncd-report.loading.branch-commits", repoDescriptor.getFullName(), branchDescriptor.getName());
            HttpRequest<?> req = getCommitsRequest(repoDescriptor, branchDescriptor, 100)
                    .queryString("since", since);
            
            List<ArrayNode> bodies = GitHubPagingHelper.pagedRequest(req, ArrayNode.class).getBodies();
            for ( ArrayNode body : bodies ) {
                for ( JsonNode commit : body ) {
                    commitsFound = true;
                    addCommit(branchCommitCollector, repoDescriptor, branchDescriptor, commit);
                }
            }
        }
        return commitsFound;
    }
    
    /**
     * Add commit data to the given {@link INcdReportRepositoryBranchCommitCollector}.
     */
    private void addCommit(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportGitLabRepositoryDescriptor repoDescriptor, NcdReportGitLabBranchDescriptor branchDescriptor, JsonNode commit) {
        var commitDescriptor = JsonHelper.treeToValue(commit, NcdReportGitLabCommitDescriptor.class);
        var authorDescriptor = JsonHelper.treeToValue(commit, NcdReportGitLabAuthorDescriptor.class);
        branchCommitCollector.reportBranchCommit(new NcdReportBranchCommitDescriptor(repoDescriptor, branchDescriptor, commitDescriptor, authorDescriptor));
    }
    
    /**
     * Get the branch descriptors for the repository described by the given
     * repository descriptor.
     */
    private List<NcdReportGitLabBranchDescriptor> getBranchDescriptors(NcdReportGitLabRepositoryDescriptor repoDescriptor) {
        List<NcdReportGitLabBranchDescriptor> result = new ArrayList<>(); 
        GitHubPagingHelper.pagedRequest(getBranchesRequest(repoDescriptor), ArrayNode.class)
            .ifSuccess(r->r.getBody().forEach(b->result.add(JsonHelper.treeToValue(b, NcdReportGitLabBranchDescriptor.class))));
        return result;
    }
    
    /**
     * Get the base request for loading commit data for the repository 
     * and branch described by the given descriptors.
     */
    private GetRequest getCommitsRequest(NcdReportGitLabRepositoryDescriptor repoDescriptor, NcdReportGitLabBranchDescriptor branchDescriptor, int perPage) {
        return unirest().get("/api/v4/projects/{projectId}/repository/commits?ref_name={branchName}")
                .routeParam("projectId", repoDescriptor.getId())
                .routeParam("branchName", branchDescriptor.getName())
                .queryString("per_page", perPage);
    }
    
    /**
     * Get the base request for loading branch data for the repository
     * described by the given repository descriptor.
     */
    private GetRequest getBranchesRequest(NcdReportGitLabRepositoryDescriptor descriptor) {
        return unirest().get(descriptor.getBranchesUrl());
    }
    
    /**
     * Convert the given {@link JsonNode} to an 
     * {@link NcdReportGitLabRepositoryDescriptor} instance.
     */
    private NcdReportGitLabRepositoryDescriptor getRepoDescriptor(JsonNode repoNode) {
        return JsonHelper.treeToValue(repoNode, NcdReportGitLabRepositoryDescriptor.class);
    }

    /**
     * Optionally configure an Authorization header to the configuration
     * of the given {@link UnirestInstance}, based on the optional
     * tokenExpression provided in the source configuration. 
     */
    @Override
    protected void configure(UnirestInstance unirest) {
        String tokenExpression = sourceConfig().getTokenExpression();
        if ( StringUtils.isNotBlank(tokenExpression) ) {
            // TODO Doesn't really make sense to use this method with null input object
            //      We should have a corresponding method in SpelHelper that doesn't take
            //      any input
            String token = JsonHelper.evaluateSpelExpression(null, tokenExpression, String.class);
            if ( StringUtils.isBlank(token) ) {
                throw new IllegalStateException("No token found from expression: "+tokenExpression);
            } else {
                unirest.config().addDefaultHeader("PRIVATE-TOKEN", token);
            }
        }
    }
    
    /**
     * Return the source type, 'gitlab' in this case.
     */
    @Override
    protected String getType() {
        return "gitlab";
    }
}
