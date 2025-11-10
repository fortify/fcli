/*
 * Copyright 2021-2025 Open Text.
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
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.license.ncd_report.collector.INcdReportRepositoryBranchCommitCollector;
import com.fortify.cli.license.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.license.ncd_report.config.NcdReportAdoOrganizationConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportAdoProjectConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportAdoSourceConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportCombinedRepoSelectorConfig;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.license.ncd_report.generator.AbstractNcdReportUnirestResultsGenerator;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public class NcdReportAdoResultsGenerator extends AbstractNcdReportUnirestResultsGenerator<NcdReportAdoSourceConfig> {
    private String currentOrganization; // Set while processing organization
    
    public NcdReportAdoResultsGenerator(NcdReportAdoSourceConfig sourceConfig, NcdReportResultsCollector resultsCollector) {
        super(sourceConfig, resultsCollector);
    }

    @Override
    protected void generateResults() {
        Stream.of(sourceConfig().getOrganizations()).forEach(this::generateResults);
    }
    
    private void generateResults(NcdReportAdoOrganizationConfig orgConfig) {
        currentOrganization = orgConfig.getName();
        for ( var projectConfig : orgConfig.getProjects() ) {
            generateResults(orgConfig, projectConfig);
        }
    }

    private void generateResults(NcdReportAdoOrganizationConfig orgConfig, NcdReportAdoProjectConfig projectConfig) {
        var orgName = orgConfig.getName();
        var projectName = projectConfig.getName();
        try {
            resultsCollector().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.ado-repositories", orgName+"/"+projectName);
            ObjectNode body = unirest().get("/{organization}/{project}/_apis/git/repositories?api-version={apiVersion}")
                .routeParam("organization", orgName)
                .routeParam("project", projectName)
                .routeParam("apiVersion", sourceConfig().getApiVersion())
                .asObject(ObjectNode.class).getBody();
            ArrayNode repos = (ArrayNode) body.path("value");
            for ( JsonNode repoNode : repos ) {
                processRepository(orgConfig, projectConfig, repoNode);
            }
        } catch ( Exception e ) {
            resultsCollector().logger().error(String.format("Error processing project: %s/%s (%s)", orgName, projectName, sourceConfig().getBaseUrl()), e);
        }
    }

    private void processRepository(NcdReportAdoOrganizationConfig orgConfig, NcdReportAdoProjectConfig projectConfig, JsonNode repoNode) {
        var repoDescriptor = getRepoDescriptor(repoNode);
        repoDescriptor.setOrganizationName(orgConfig.getName());
        var combinedSelectorOrg = new NcdReportCombinedRepoSelectorConfig(sourceConfig(), orgConfig);
        var combinedSelectorProj = new NcdReportCombinedRepoSelectorConfig(combinedSelectorOrg, projectConfig);
        resultsCollector().repositoryProcessor().processRepository(combinedSelectorProj, repoDescriptor, this::generateCommitData);
    }

    private void generateCommitData(NcdReportAdoRepositoryDescriptor repoDescriptor, INcdReportRepositoryBranchCommitCollector branchCommitCollector) {
        var branchDescriptors = getBranchDescriptors(repoDescriptor);
        boolean commitsFound = generateCommitDataForBranches(branchCommitCollector, repoDescriptor, branchDescriptors);
        if ( !commitsFound ) {
            generateMostRecentCommitData(branchCommitCollector, repoDescriptor, branchDescriptors);
        }
    }

    private void generateMostRecentCommitData(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportAdoRepositoryDescriptor repoDescriptor, List<NcdReportAdoBranchDescriptor> branchDescriptors) {
        NcdReportAdoCommitDescriptor mostRecentCommit = null;
        NcdReportAdoBranchDescriptor mostRecentBranch = null;
        for ( var branch : branchDescriptors ) {
            ObjectNode body = getCommitsRequest(repoDescriptor, branch, 1, null).asObject(ObjectNode.class).getBody();
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

    private boolean generateCommitDataForBranches(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportAdoRepositoryDescriptor repoDescriptor, List<NcdReportAdoBranchDescriptor> branchDescriptors) {
        String since = resultsCollector().reportConfig().getCommitOffsetDateTime().format(DateTimeFormatter.ISO_INSTANT);
        boolean commitsFound = false;
        for ( var branchDescriptor : branchDescriptors ) {
            resultsCollector().progressWriter().writeI18nProgress("fcli.license.ncd-report.loading.branch-commits", repoDescriptor.getFullName(), branchDescriptor.getName());
            ObjectNode body = getCommitsRequest(repoDescriptor, branchDescriptor, 100, since).asObject(ObjectNode.class).getBody();
            ArrayNode commits = (ArrayNode) body.path("value");
            for ( JsonNode commit : commits ) {
                commitsFound = true;
                addCommit(branchCommitCollector, repoDescriptor, branchDescriptor, commit);
            }
        }
        return commitsFound;
    }

    private void addCommit(INcdReportRepositoryBranchCommitCollector branchCommitCollector, NcdReportAdoRepositoryDescriptor repoDescriptor, NcdReportAdoBranchDescriptor branchDescriptor, JsonNode commit) {
        var commitDescriptor = JsonHelper.treeToValue(commit, NcdReportAdoCommitDescriptor.class);
        var authorDescriptor = JsonHelper.treeToValue(commit, NcdReportAdoAuthorDescriptor.class);
        branchCommitCollector.reportBranchCommit(new NcdReportBranchCommitDescriptor(repoDescriptor, branchDescriptor, commitDescriptor, authorDescriptor));
    }
    
    private List<NcdReportAdoBranchDescriptor> getBranchDescriptors(NcdReportAdoRepositoryDescriptor repoDescriptor) {
        List<NcdReportAdoBranchDescriptor> result = new ArrayList<>();
        ObjectNode body = getBranchesRequest(repoDescriptor).asObject(ObjectNode.class).getBody();
        ArrayNode branches = (ArrayNode) body.path("value");
        for ( JsonNode branch : branches ) {
            result.add(JsonHelper.treeToValue(branch, NcdReportAdoBranchDescriptor.class));
        }
        return result;
    }

    private GetRequest getCommitsRequest(NcdReportAdoRepositoryDescriptor repoDescriptor, NcdReportAdoBranchDescriptor branchDescriptor, int top, String fromDate) {
        GetRequest req = unirest().get("/{organization}/{project}/_apis/git/repositories/{repoId}/commits?searchCriteria.itemVersion.version={branchName}&searchCriteria.itemVersion.versionType=branch&api-version={apiVersion}")
                .routeParam("organization", currentOrganization)
                .routeParam("project", repoDescriptor.getProjectName())
                .routeParam("repoId", repoDescriptor.getId())
                .routeParam("branchName", branchDescriptor.getName())
                .routeParam("apiVersion", sourceConfig().getApiVersion())
                .queryString("searchCriteria.$top", top);
        if ( fromDate!=null ) {
            req = req.queryString("searchCriteria.fromDate", fromDate);
        }
        return req;
    }

    private GetRequest getBranchesRequest(NcdReportAdoRepositoryDescriptor repoDescriptor) {
        return unirest().get("/{organization}/{project}/_apis/git/repositories/{repoId}/refs?filter=heads/&api-version={apiVersion}")
                .routeParam("organization", currentOrganization)
                .routeParam("project", repoDescriptor.getProjectName())
                .routeParam("repoId", repoDescriptor.getId())
                .routeParam("apiVersion", sourceConfig().getApiVersion());
    }

    private NcdReportAdoRepositoryDescriptor getRepoDescriptor(JsonNode repoNode) {
        return JsonHelper.treeToValue(repoNode, NcdReportAdoRepositoryDescriptor.class);
    }

    @Override
    protected void configure(UnirestInstance unirest) {
        String tokenExpression = sourceConfig().getTokenExpression();
        if ( StringUtils.isNotBlank(tokenExpression) ) {
            String token = JsonHelper.evaluateSpelExpression(null, tokenExpression, String.class);
            if ( StringUtils.isBlank(token) ) {
                throw new FcliSimpleException("No token found from expression: "+tokenExpression);
            } else {
                String basic = java.util.Base64.getEncoder().encodeToString((":"+token).getBytes());
                unirest.config().setDefaultHeader("Authorization", "Basic "+basic);
            }
        }
    }

    @Override
    protected String getType() { return "ado"; }
}
