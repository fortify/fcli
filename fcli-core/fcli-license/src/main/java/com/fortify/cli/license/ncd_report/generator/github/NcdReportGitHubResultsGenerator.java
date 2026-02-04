/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.ncd_report.generator.github;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.ci.github.GitHubRestHelper;
import com.fortify.cli.common.ci.github.GitHubUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryProcessor;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.config.NcdReportCombinedRepoSelectorConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportGitHubOrganizationConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportGitHubSourceConfig;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.license.ncd_report.generator.AbstractNcdReportResultsGenerator;

/**
 * This class is responsible for loading repository, branch, commit and author
 * data from GitHub.
 * 
 * @author rsenden
 *
 */
public class NcdReportGitHubResultsGenerator extends AbstractNcdReportResultsGenerator<NcdReportGitHubSourceConfig> {
    /**
     * REST helper for GitHub API operations. A new instance is created for each
     * source configuration, ensuring proper isolation of API credentials and
     * configuration (base URL, token, etc.).
     */
    private final GitHubRestHelper restHelper;
    
    /**
     * Constructor to configure this instance with the given 
     * {@link NcdReportGitHubSourceConfig} and
     * {@link NcdReportContext}.
     */
    public NcdReportGitHubResultsGenerator(NcdReportGitHubSourceConfig sourceConfig, NcdReportContext reportContext) {        
        super(sourceConfig, reportContext);
        this.restHelper = createRestHelper(sourceConfig, reportContext);
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
     * by our {@link NcdReportContext}. The {@link INcdReportRepositoryProcessor}
     * will in turn call our {@link #generateCommitData(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor)}
     * method to generate commit data for every repository that is not excluded from
     * the report.
     */
    private void generateResults(NcdReportGitHubOrganizationConfig orgConfig) {
        String orgName = orgConfig.getName();
        try {
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.github-repositories", orgName);
            restHelper.processRepositories(orgName, repo -> {
                reportContext().repositoryProcessor().processRepository(
                    new NcdReportCombinedRepoSelectorConfig(sourceConfig(), orgConfig),
                    getRepoDescriptor(repo),
                    this::generateCommitData);
                return Break.FALSE;
            });
        } catch ( Exception e ) {
            reportContext().logger().error(String.format("Error processing organization: %s (%s)", orgName, sourceConfig().getApiUrl()), e);
        }
    }
    
    /**
     * This method generates commit data for the given repository by retrieving
     * all branches, and then invoking the {@link #generateCommitDataForBranches(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor, List)}
     * method to generate commit data for each branch. If no commits are found that
     * match the date range, the {@link #generateMostRecentCommitData(INcdReportRepositoryBranchCommitCollector, NcdReportGitHubRepositoryDescriptor, List)}
     * method is invoked to find the most recent commit older than the date range.
     */
    private void generateCommitData(NcdReportGitHubRepositoryDescriptor repoDescriptor, INcdReportRepositoryBranchCommitCollector branchCommitCollector) {
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
            var currentCommitResponse = getLatestCommit(repoDescriptor, branchDescriptor);
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
        String since = reportContext().reportConfig().getCommitOffsetDateTime()
                .format(DateTimeFormatter.ISO_INSTANT);
        boolean commitsFound = false;
        for ( var branchDescriptor : branchDescriptors ) {
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.branch-commits", repoDescriptor.getFullName(), branchDescriptor.getName());
            List<Boolean> foundFlag = new ArrayList<>();
            restHelper.processCommits(repoDescriptor.getOwnerName(), repoDescriptor.getName(), 
                branchDescriptor.getSha(), since, commit -> {
                    foundFlag.add(true);
                    addCommit(branchCommitCollector, repoDescriptor, branchDescriptor, commit);
                    return Break.FALSE;
                });
            if (!foundFlag.isEmpty()) {
                commitsFound = true;
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
        restHelper.processBranches(repoDescriptor.getOwnerName(), repoDescriptor.getName(), 
            branch -> {
                result.add(JsonHelper.treeToValue(branch, NcdReportGitHubBranchDescriptor.class));
                return Break.FALSE;
            });
        return result;
    }
    
    /**
     * Get a single commit (most recent) for a branch.
     */
    private ArrayNode getLatestCommit(NcdReportGitHubRepositoryDescriptor repoDescriptor, NcdReportGitHubBranchDescriptor branchDescriptor) {
        return restHelper.getLatestCommit(repoDescriptor.getOwnerName(), repoDescriptor.getName(), branchDescriptor.getSha());
    }
    
    /**
     * Convert the given {@link JsonNode} to an 
     * {@link NcdReportGitHubRepositoryDescriptor} instance.
     */
    private NcdReportGitHubRepositoryDescriptor getRepoDescriptor(JsonNode repoNode) {
        return JsonHelper.treeToValue(repoNode, NcdReportGitHubRepositoryDescriptor.class);
    }

    /**
     * Create and configure GitHubRestHelper with UnirestContext, URL config, and auth token.
     */
    private static GitHubRestHelper createRestHelper(NcdReportGitHubSourceConfig sourceConfig, NcdReportContext reportContext) {
        var supplierBuilder = GitHubUnirestInstanceSupplier.builder(reportContext.unirestContext());
        
        // Configure URL config (includes URL, timeouts, SSL verification)
        if (sourceConfig.hasUrlConfig()) {
            supplierBuilder.urlConfig(sourceConfig);
        }
        
        // Configure token if provided
        String tokenExpression = sourceConfig.getTokenExpression();
        if (StringUtils.isNotBlank(tokenExpression)) {
            String token = JsonHelper.evaluateSpelExpression(null, tokenExpression, String.class);
            if (StringUtils.isBlank(token)) {
                throw new FcliSimpleException("No token found from expression: " + tokenExpression);
            }
            supplierBuilder.token(token);
        }
        
        return new GitHubRestHelper(supplierBuilder.build());
    }

    /**
     * Return the source type, 'github' in this case.
     */
    @Override
    protected String getType() {
        return "github";
    }
}
