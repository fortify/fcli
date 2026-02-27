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
package com.fortify.cli.license.ncd_report.generator.gitlab;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.ci.gitlab.GitLabRestHelper;
import com.fortify.cli.common.ci.gitlab.GitLabUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryProcessor;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.config.NcdReportCombinedRepoSelectorConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportGitLabGroupConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportGitLabSourceConfig;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.license.ncd_report.generator.AbstractNcdReportResultsGenerator;

/**
 * This class is responsible for loading repository, branch, commit and author
 * data from GitLab.
 * 
 * @author rsenden
 *
 */
public class NcdReportGitLabResultsGenerator extends AbstractNcdReportResultsGenerator<NcdReportGitLabSourceConfig> {
    /**
     * REST helper for GitLab API operations. A new instance is created for each
     * source configuration, ensuring proper isolation of API credentials and
     * configuration (base URL, token, etc.).
     */
    private final GitLabRestHelper restHelper;
    
    /**
     * Constructor to configure this instance with the given 
     * {@link NcdReportGitLabSourceConfig} and
     * {@link NcdReportContext}.
     */
    public NcdReportGitLabResultsGenerator(NcdReportGitLabSourceConfig sourceConfig, NcdReportContext reportContext) {        
        super(sourceConfig, reportContext);
        this.restHelper = createRestHelper(sourceConfig, reportContext);
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
     * by our {@link NcdReportContext}. The {@link INcdReportRepositoryProcessor}
     * will in turn call our {@link #generateCommitData(NcdReportGitLabRepositoryDescriptor, INcdReportRepositoryBranchCommitCollector)}
     * method to generate commit data for every repository that is not excluded from
     * the report.
     */
    private void generateResults(NcdReportGitLabGroupConfig groupConfig) {
        String groupId = groupConfig.getId();
        try {
            boolean includeSubgroups = groupConfig.getIncludeSubgroups().orElse(sourceConfig().getIncludeSubgroups().orElse(true));
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.gitlab-repositories", groupId);
            restHelper.group(groupId).queryProjects().includeSubgroups(includeSubgroups).process(project -> {
                reportContext().repositoryProcessor().processRepository(
                    new NcdReportCombinedRepoSelectorConfig(sourceConfig(), groupConfig),
                    getRepoDescriptor(project),
                    this::generateCommitData);
                return Break.FALSE;
            });
        } catch ( Exception e ) {
            reportContext().logger().error(String.format("Error processing group: %s (%s)", groupId, sourceConfig().getBaseUrl()), e);
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
            var currentCommitResponse = getLatestCommit(repoDescriptor, branchDescriptor);
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
        String since = reportContext().reportConfig().getCommitOffsetDateTime()
                .format(DateTimeFormatter.ISO_INSTANT);
        boolean commitsFound = false;
        for ( var branchDescriptor : branchDescriptors ) {
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.branch-commits", repoDescriptor.getFullName(), branchDescriptor.getName());
            List<Boolean> foundFlag = new ArrayList<>();
            restHelper.project(repoDescriptor.getId())
                .queryCommits().refName(branchDescriptor.getName()).since(since).process(commit -> {
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
        restHelper.project(repoDescriptor.getId())
            .queryBranches().process(branch -> {
                result.add(JsonHelper.treeToValue(branch, NcdReportGitLabBranchDescriptor.class));
                return Break.FALSE;
            });
        return result;
    }
    
    /**
     * Get a single commit (most recent) for a branch.
     */
    private ArrayNode getLatestCommit(NcdReportGitLabRepositoryDescriptor repoDescriptor, NcdReportGitLabBranchDescriptor branchDescriptor) {
        return restHelper.project(repoDescriptor.getId())
            .getLatestCommit(branchDescriptor.getName());
    }
    
    /**
     * Convert the given {@link JsonNode} to an 
     * {@link NcdReportGitLabRepositoryDescriptor} instance.
     */
    private NcdReportGitLabRepositoryDescriptor getRepoDescriptor(JsonNode repoNode) {
        return JsonHelper.treeToValue(repoNode, NcdReportGitLabRepositoryDescriptor.class);
    }

    /**
     * Create and configure GitLabRestHelper with UnirestContext, URL config, and auth token.
     */
    private static GitLabRestHelper createRestHelper(NcdReportGitLabSourceConfig sourceConfig, NcdReportContext reportContext) {
        var supplierBuilder = GitLabUnirestInstanceSupplier.builder(reportContext.unirestContext());
        
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
        
        return new GitLabRestHelper(supplierBuilder.build());
    }
    
    /**
     * Return the source type, 'gitlab' in this case.
     */
    @Override
    protected String getType() {
        return "gitlab";
    }
}
