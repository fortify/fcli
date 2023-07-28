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
package com.fortify.cli.license.ncd_report.collector;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.license.ncd_report.config.INcdReportRepoSelectorConfig;
import com.fortify.cli.license.ncd_report.config.NcdReportConfig;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.license.ncd_report.generator.INcdReportBranchCommitGenerator;
import com.fortify.cli.license.ncd_report.writer.NcdReportResultsWriters;
import com.fortify.cli.license.ncd_report.writer.NcdReportRepositoriesWriter.NcdReportRepositoryReportingStatus;

/**
 * <p>This class is responsible for processing repositories passed to 
 * the {@link #processRepository(INcdReportRepoSelectorConfig, INcdReportRepositoryDescriptor, INcdReportBranchCommitGenerator)
 * method, implementing the {@link INcdReportRepositoryProcessor} interface.
 * It is responsible for determining from which repositories commit data
 * should be loaded based on the configured {@link NcdReportConfig} and
 * provided {@link INcdReportRepoSelectorConfig}. For each repository for
 * which commit data should be loaded, the provided, source-specific 
 * {@link INcdReportBranchCommitGenerator} is called to generate commit data,
 * which is then further processed using {@link NcdReportRepositoryBranchCommitCollector}
 * and {@link NcdReportAuthorCollector}.
 * 
 * TODO: We currently only support a standard include expression for all sources;
 *       potentially we need to have callback functionality to allow each source
 *       to make additional decisions as to what repositories to include or exclude,
 *       based on source-specific configuration settings.
 * @author rsenden
 *
 */
final class NcdReportRepositoryProcessor implements INcdReportRepositoryProcessor {
    private final NcdReportConfig reportConfig;
    private final NcdReportResultsWriters writers;
    private final ObjectNode summary;
    private final NcdReportRepositoryCollector repositoryCollector;
    private final NcdReportAuthorCollector authorCollector;
    
    private int totalAnalyzedCommitCount = 0;
    
    public NcdReportRepositoryProcessor(NcdReportConfig reportConfig, NcdReportResultsWriters writers, ObjectNode summary) {
        this.reportConfig = reportConfig;
        this.writers = writers;
        this.summary = summary;
        this.repositoryCollector = new NcdReportRepositoryCollector(writers, summary);
        this.authorCollector = new NcdReportAuthorCollector(reportConfig, writers, summary);
    }
    
    @Override
    public <R extends INcdReportRepositoryDescriptor> void processRepository(
            INcdReportRepoSelectorConfig repoSelectorConfig, 
            R repoDescriptor, INcdReportBranchCommitGenerator<R> commitGenerator) 
    {
        if ( !repositoryCollector.isPreviouslyReported(repoDescriptor) ) {
            try {
                if ( isExcludedFork(repoDescriptor, reportConfig, repoSelectorConfig) ) {
                    repositoryCollector.reportRepository(repoDescriptor, NcdReportRepositoryReportingStatus.excluded, "Forks not included");
                } else if ( isExcludedByExpression(repoDescriptor, reportConfig, repoSelectorConfig) ) {
                    repositoryCollector.reportRepository(repoDescriptor, NcdReportRepositoryReportingStatus.excluded, "Doesn't match expression");
                } else {
                    processRepository(repoDescriptor, commitGenerator);
                }
            } catch ( Exception e ) {
                repositoryCollector.reportRepositoryError(repoDescriptor, e);
            }
        }
    }

    private <R extends INcdReportRepositoryDescriptor> void processRepository(R repoDescriptor, INcdReportBranchCommitGenerator<R> branchCommitGenerator) {
        var branchCommitsCollector = new NcdReportRepositoryBranchCommitCollector(authorCollector, repoDescriptor);
        writers.progressWriter().writeI18nProgress("fcli.util.ncd-report.loading.commits", repoDescriptor.getFullName());
        branchCommitGenerator.generateBranchCommitData(repoDescriptor, branchCommitsCollector);
        if ( branchCommitsCollector.isEmpty() ) {
            repositoryCollector.reportRepository(repoDescriptor, NcdReportRepositoryReportingStatus.empty, "No commits found");
        } else {
            branchCommitsCollector.writeResults(writers);
            totalAnalyzedCommitCount+=branchCommitsCollector.getTotalCommitCount();
            repositoryCollector.reportRepository(repoDescriptor, NcdReportRepositoryReportingStatus.included, "Matches all criteria");
        }
    }

    private boolean isExcludedFork(INcdReportRepositoryDescriptor repoDescriptor, NcdReportConfig reportConfig, INcdReportRepoSelectorConfig repoSelector) {
        return repoDescriptor.isFork() && isExcludeForks(reportConfig, repoSelector);
    }

    private boolean isExcludeForks(NcdReportConfig reportConfig, INcdReportRepoSelectorConfig repoSelector) {
        return !repoSelector.getIncludeForks()
                .orElse(reportConfig.getSources().getIncludeForks().orElse(false));
    }
    
    private boolean isExcludedByExpression(INcdReportRepositoryDescriptor repoDescriptor, NcdReportConfig reportConfig, INcdReportRepoSelectorConfig repoSelector) {
        return repoSelector.getRepositoryIncludeExpression()
            .filter(StringUtils::isNotBlank)
            .map(expr->!JsonHelper.evaluateSpelExpression(repoDescriptor.asJsonNode(), expr, Boolean.class))
            .orElse(false);
    }
    
    void writeResults() {
        repositoryCollector.writeResults();
        summary.set("commitCount", JsonHelper.getObjectMapper().createObjectNode()
                .put("analyzed", totalAnalyzedCommitCount));
        authorCollector.writeResults();
    }
}
