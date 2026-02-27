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
package com.fortify.cli.license.ncd_report.generator.ado;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.ci.ado.AdoRestHelper;
import com.fortify.cli.common.ci.ado.AdoUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.config.NcdReportAdoOrganizationConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportAdoProjectConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportAdoSourceConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportCombinedRepoSelectorConfig;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.license.ncd_report.generator.AbstractNcdReportResultsGenerator;

public class NcdReportAdoResultsGenerator extends AbstractNcdReportResultsGenerator<NcdReportAdoSourceConfig> {
    
    public NcdReportAdoResultsGenerator(NcdReportAdoSourceConfig sourceConfig, NcdReportContext reportContext) {
        super(sourceConfig, reportContext);
        // Note: REST helper is created per-organization, not here
    }

    @Override
    protected void generateResults() {
        Stream.of(sourceConfig().getOrganizations()).forEach(this::generateResults);
    }
    
    private void generateResults(NcdReportAdoOrganizationConfig orgConfig) {
        // Create organization-specific REST helper with full URL including organization
        var restHelper = createRestHelper(sourceConfig(), reportContext(), orgConfig.getName());
        var projects = orgConfig.getProjects();
        if ( projects == null || projects.length == 0 ) {
            // No projects configured - discover all projects in organization
            generateResultsForAllProjects(restHelper, orgConfig);
        } else {
            // Process configured projects
            for ( var projectConfig : projects ) {
                generateResults(restHelper, orgConfig, projectConfig);
            }
        }
    }
    
    private void generateResultsForAllProjects(AdoRestHelper restHelper, NcdReportAdoOrganizationConfig orgConfig) {
        var orgName = orgConfig.getName();
        try {
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.ado-projects", orgName);
            restHelper.organization(orgName).queryProjects().process(projectNode -> {
                var projectName = projectNode.get("name").asText();
                var projectConfig = new NcdReportAdoProjectConfig();
                projectConfig.setName(projectName);
                generateResults(restHelper, orgConfig, projectConfig);
                return Break.FALSE;
            });
        } catch ( Exception e ) {
            reportContext().logger().error(String.format("Error processing organization: %s (%s)", orgName, sourceConfig().getBaseUrl()), e);
        }
    }

    private void generateResults(AdoRestHelper restHelper, NcdReportAdoOrganizationConfig orgConfig, NcdReportAdoProjectConfig projectConfig) {
        var orgName = orgConfig.getName();
        var projectName = projectConfig.getName();
        try {
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.ado-repositories", orgName+"/"+projectName);
            restHelper.project(orgName, projectName).queryRepositories().process(repoNode -> {
                processRepository(restHelper, orgConfig, projectConfig, repoNode);
                return Break.FALSE;
            });
        } catch ( Exception e ) {
            reportContext().logger().error(String.format("Error processing project: %s/%s (%s)", orgName, projectName, sourceConfig().getBaseUrl()), e);
        }
    }

    private void processRepository(AdoRestHelper restHelper, NcdReportAdoOrganizationConfig orgConfig, NcdReportAdoProjectConfig projectConfig, JsonNode repoNode) {
        var repoDescriptor = getRepoDescriptor(repoNode);
        repoDescriptor.setOrganizationName(orgConfig.getName());
        var combinedSelectorOrg = new NcdReportCombinedRepoSelectorConfig(sourceConfig(), orgConfig);
        var combinedSelectorProj = new NcdReportCombinedRepoSelectorConfig(combinedSelectorOrg, projectConfig);
        reportContext().repositoryProcessor().processRepository(combinedSelectorProj, repoDescriptor, 
            (repoDesc, collector) -> generateCommitData(restHelper, repoDesc, collector));
    }

    private void generateCommitData(AdoRestHelper restHelper, NcdReportAdoRepositoryDescriptor repoDescriptor, INcdReportRepositoryBranchCommitCollector branchCommitCollector) {
        var branchDescriptors = getBranchDescriptors(restHelper, repoDescriptor);
        boolean commitsFound = generateCommitDataForBranches(restHelper, branchCommitCollector, repoDescriptor, branchDescriptors);
        if ( !commitsFound ) {
            generateMostRecentCommitData(restHelper, branchCommitCollector, repoDescriptor, branchDescriptors);
        }
    }

    private void generateMostRecentCommitData(AdoRestHelper restHelper, INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportAdoRepositoryDescriptor repoDescriptor, List<NcdReportAdoBranchDescriptor> branchDescriptors) {
        NcdReportAdoCommitDescriptor mostRecentCommit = null;
        NcdReportAdoBranchDescriptor mostRecentBranch = null;
        for ( var branch : branchDescriptors ) {
            ObjectNode body = getLatestCommit(restHelper, repoDescriptor, branch);
            ArrayNode commits = (ArrayNode) body.path("value");
            if ( commits.size()>0 ) {
                var commit = JsonHelper.treeToValue(commits.get(0), NcdReportAdoCommitDescriptor.class);
                if ( mostRecentCommit==null || commit.getDate().isAfter(mostRecentCommit.getDate()) ) {
                    mostRecentCommit = commit;
                    mostRecentBranch = branch;
                }
            }
        }
        if ( mostRecentCommit!=null ) {
            addCommit(branchCommitCollector, repoDescriptor, mostRecentBranch, mostRecentCommit.asJsonNode());
        }
    }

    private boolean generateCommitDataForBranches(AdoRestHelper restHelper, INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportAdoRepositoryDescriptor repoDescriptor, List<NcdReportAdoBranchDescriptor> branchDescriptors) {
        String since = reportContext().reportConfig().getCommitOffsetDateTime().format(DateTimeFormatter.ISO_INSTANT);
        boolean commitsFound = false;
        for ( var branchDescriptor : branchDescriptors ) {
            reportContext().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.branch-commits", repoDescriptor.getFullName(), branchDescriptor.getName());
            List<Boolean> foundFlag = new ArrayList<>();
            restHelper.repository(repoDescriptor.getOrganizationName(), repoDescriptor.getProjectName(), repoDescriptor.getId())
                .queryCommits().branchName(branchDescriptor.getName()).fromDate(since).process(commit -> {
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

    private void addCommit(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportAdoRepositoryDescriptor repoDescriptor, NcdReportAdoBranchDescriptor branchDescriptor, JsonNode commit) {
        var commitDescriptor = JsonHelper.treeToValue(commit, NcdReportAdoCommitDescriptor.class);
        var authorDescriptor = JsonHelper.treeToValue(commit, NcdReportAdoAuthorDescriptor.class);
        branchCommitCollector.reportBranchCommit(new NcdReportBranchCommitDescriptor(repoDescriptor, branchDescriptor, commitDescriptor, authorDescriptor));
    }
    
    private List<NcdReportAdoBranchDescriptor> getBranchDescriptors(AdoRestHelper restHelper, NcdReportAdoRepositoryDescriptor repoDescriptor) {
        List<NcdReportAdoBranchDescriptor> result = new ArrayList<>();
        restHelper.repository(repoDescriptor.getOrganizationName(), repoDescriptor.getProjectName(), repoDescriptor.getId())
            .queryBranches().process(branch -> {
                result.add(JsonHelper.treeToValue(branch, NcdReportAdoBranchDescriptor.class));
                return Break.FALSE;
            });
        return result;
    }

    private ObjectNode getLatestCommit(AdoRestHelper restHelper, NcdReportAdoRepositoryDescriptor repoDescriptor, NcdReportAdoBranchDescriptor branchDescriptor) {
        return restHelper.repository(repoDescriptor.getOrganizationName(), repoDescriptor.getProjectName(), repoDescriptor.getId())
            .getLatestCommit(branchDescriptor.getName());
    }

    private NcdReportAdoRepositoryDescriptor getRepoDescriptor(JsonNode repoNode) {
        return JsonHelper.treeToValue(repoNode, NcdReportAdoRepositoryDescriptor.class);
    }

    /**
     * Create and configure AdoRestHelper with UnirestContext, URL config (including organization), and auth token.
     * 
     * @param sourceConfig Source configuration containing base URL and connection settings
     * @param reportContext Report context containing UnirestContext
     * @param organizationName Organization/collection name to append to base URL
     * @return Configured AdoRestHelper instance for this organization
     */
    private static AdoRestHelper createRestHelper(NcdReportAdoSourceConfig sourceConfig, NcdReportContext reportContext, String organizationName) {
        var supplierBuilder = AdoUnirestInstanceSupplier.builder(reportContext.unirestContext());
        
        // Build complete URL with organization: baseUrl + "/" + organization
        // e.g., https://dev.azure.com/fabrikamfiber
        if (sourceConfig.hasUrlConfig()) {
            String baseUrl = sourceConfig.getUrl();
            String fullUrl = baseUrl.endsWith("/") 
                ? baseUrl + organizationName 
                : baseUrl + "/" + organizationName;
            
            var urlConfig = UrlConfig.builderFrom(sourceConfig)
                .url(fullUrl)
                .build();
            supplierBuilder.urlConfig(urlConfig);
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
        
        return new AdoRestHelper(supplierBuilder.build());
    }

    @Override
    protected String getType() { return "ado"; }
}
