/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.ncd_report.generator.github;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.util.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.util.ncd_report.collector.INcdReportRepositoryProcessor;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.util.ncd_report.config.NcdReportCombinedRepoSelectorConfig;
import com.fortify.cli.util.ncd_report.config.NcdReportGitHubOrganizationConfig;
import com.fortify.cli.util.ncd_report.config.NcdReportGitHubSourceConfig;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.util.ncd_report.generator.AbstractNcdReportUnirestResultsGenerator;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

/**
 * This class is responsible for loading repository, branch, commit and author
 * data from GitHub.
 * 
 * @author rsenden
 *
 */
public class NcdReportGitHubResultsGenerator extends AbstractNcdReportUnirestResultsGenerator<NcdReportGitHubSourceConfig> {
    /**
     * Constructor to configure this instance with the given 
     * {@link NcdReportGitHubSourceConfig} and
     * {@link NcdReportResultsCollector}.
     */
    public NcdReportGitHubResultsGenerator(NcdReportGitHubSourceConfig sourceConfig, NcdReportResultsCollector resultsCollector) {        
        super(sourceConfig, resultsCollector);
    }

    /**
     * Primary method for generating report results. This gets the 
     * organization configurations, and for each organization, calls 
     * the {@link #generateResults(NcdReportGitHubOrganizationConfig)}
     * method to load the repositories for that organization.
     */
    @Override
    protected void generateResults() {
        Stream.of(sourceConfig().getOrganizations()).forEach(this::generateResults);
    }
    
    /**
     * This method loads the repositories for the organization specified in the
     * given {@link NcdReportGitHubOrganizationConfig}, and passes the descriptor
     * for each repository to the {@link INcdReportRepositoryProcessor} provided 
     * by our {@link NcdReportResultsCollector}. The {@link INcdReportRepositoryProcessor}
     * will in turn call our {@link #generateCommitData(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor)}
     * method to generate commit data for every repository that is not excluded from
     * the report.
     */
    private void generateResults(NcdReportGitHubOrganizationConfig orgConfig) {
        String orgName = orgConfig.getName();
        try {
            resultsCollector().progressWriter().writeI18nProgress("fcli.util.ncd-report.loading.github-repositories", orgName);
            HttpRequest<?> req = unirest().get("/orgs/{org}/repos?type=all&per_page=100").routeParam("org", orgName);
            GitHubPagingHelper.pagedRequest(req, ArrayNode.class)
                .ifSuccess(r->r.getBody().forEach(repo->
                    resultsCollector().repositoryProcessor().processRepository(new NcdReportCombinedRepoSelectorConfig(sourceConfig(), orgConfig), getRepoDescriptor(repo), this::generateCommitData)));
        } catch ( Exception e ) {
            resultsCollector().logger().error(String.format("Error processing organization: %s (%s)", orgName, sourceConfig().getApiUrl()), e);
        }
    }
    
    /**
     * This method generates commit data for the given repository by retrieving
     * all branches, and then invoking the {@link #generateCommitDataForBranches(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor, List)}
     * method to generate commit data for each branch. If no commits are found that
     * match the date range, the {@link #generateMostRecentCommitData(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor, List)}
     * method is invoked to find the most recent commit older than the date range.
     */
    private void generateCommitData(NcdReportGitHubRepositoryDescriptor repoDescriptor,INcdReportRepositoryBranchCommitCollector branchCommitCollector) {
        var branchDescriptors = getBranchDescriptors(repoDescriptor);
        boolean commitsFound = generateCommitDataForBranches(branchCommitCollector, repoDescriptor, branchDescriptors);
        if ( !commitsFound ) {
            generateMostRecentCommitData(branchCommitCollector, repoDescriptor, branchDescriptors);
        }
    }

    /**
     * This method loads the latest commit for every branch, then passes the overall
     * latest commit (if found) to the {@link #addCommit(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor, NcdReportGitHubBranchDescriptor, JsonNode)}
     * method.
     */
    private void generateMostRecentCommitData(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportGitHubRepositoryDescriptor repoDescriptor, List<NcdReportGitHubBranchDescriptor> branchDescriptors) {
        NcdReportGitHubCommitDescriptor mostRecentCommitDescriptor = null;
        NcdReportGitHubBranchDescriptor mostRecentBranchDescriptor = null;
        for ( var branchDescriptor : branchDescriptors ) {
            var currentCommitResponse = getCommitsRequest(repoDescriptor, branchDescriptor, 1)
                .asObject(ArrayNode.class).getBody();
            if ( currentCommitResponse.size()>0 ) {
                var currentCommitDescriptor = JsonHelper.treeToValue(currentCommitResponse.get(0), NcdReportGitHubCommitDescriptor.class);
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
    private boolean generateCommitDataForBranches(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportGitHubRepositoryDescriptor repoDescriptor, List<NcdReportGitHubBranchDescriptor> branchDescriptors) {
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
    private void addCommit(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportGitHubRepositoryDescriptor repoDescriptor, NcdReportGitHubBranchDescriptor branchDescriptor, JsonNode commit) {
        var commitDescriptor = JsonHelper.treeToValue(commit, NcdReportGitHubCommitDescriptor.class);
        var authorDescriptor = JsonHelper.treeToValue(commit, NcdReportGitHubAuthorDescriptor.class);
        branchCommitCollector.reportBranchCommit(new NcdReportBranchCommitDescriptor(repoDescriptor, branchDescriptor, commitDescriptor, authorDescriptor));
    }
    
    /**
     * Get the branch descriptors for the repository described by the given
     * repository descriptor.
     */
    private List<NcdReportGitHubBranchDescriptor> getBranchDescriptors(NcdReportGitHubRepositoryDescriptor repoDescriptor) {
        List<NcdReportGitHubBranchDescriptor> result = new ArrayList<>(); 
        GitHubPagingHelper.pagedRequest(getBranchesRequest(repoDescriptor), ArrayNode.class)
            .ifSuccess(r->r.getBody().forEach(b->result.add(JsonHelper.treeToValue(b, NcdReportGitHubBranchDescriptor.class))));
        return result;
    }
    
    /**
     * Get the base request for loading commit data for the repository 
     * and branch described by the given descriptors.
     */
    private GetRequest getCommitsRequest(NcdReportGitHubRepositoryDescriptor descriptor, NcdReportGitHubBranchDescriptor branchDescriptor, int perPage) {
        return unirest().get("/repos/{owner}/{repo}/commits")
                .routeParam("owner", descriptor.getOwnerName())
                .routeParam("repo", descriptor.getName())
                .queryString("sha", branchDescriptor.getSha())
                .queryString("per_page", perPage);
    }
    
    /**
     * Get the base request for loading branch data for the repository
     * described by the given repository descriptor.
     */
    private GetRequest getBranchesRequest(NcdReportGitHubRepositoryDescriptor descriptor) {
        return unirest().get("/repos/{owner}/{repo}/branches?per_page=100")
                .routeParam("owner", descriptor.getOwnerName())
                .routeParam("repo", descriptor.getName());
    }
    
    /**
     * Convert the given {@link JsonNode} to an 
     * {@link NcdReportGitHubRepositoryDescriptor} instance.
     */
    private NcdReportGitHubRepositoryDescriptor getRepoDescriptor(JsonNode repoNode) {
        return JsonHelper.treeToValue(repoNode, NcdReportGitHubRepositoryDescriptor.class);
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
                unirest.config().setDefaultHeader("Authorization", "Bearer "+token);
            }
        }
    }
    
    /**
     * Return the source type, 'github' in this case.
     */
    @Override
    protected String getType() {
        return "github";
    }
}
