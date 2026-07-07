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
package com.fortify.cli.license.ncd_report.collector;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fortify.cli.license.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportSummaryDescriptor;
import com.fortify.cli.license.ncd_report.writer.NcdReportRepositoriesWriter.NcdReportRepositoryReportingStatus;
import com.fortify.cli.license.ncd_report.writer.NcdReportResultsWriters;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting and outputting 
 * {@link INcdReportRepositoryDescriptor} instances as reported 
 * by {@link NcdReportRepositoryProcessor}.</p>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
final class NcdReportRepositoryCollector {
    private final NcdReportResultsWriters writers;
    private final NcdReportSummaryDescriptor summary;
    
    private Set<INcdReportRepositoryDescriptor> repositories = new LinkedHashSet<>();
    private Map<NcdReportRepositoryReportingStatus, Integer> repositoryCountsByStatus = new HashMap<>();
    private int dormantRepositoryCount = 0;
    
    @SneakyThrows
    void reportRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason) {
        reportRepository(descriptor, status, reason, null);
    }

    @SneakyThrows
    void reportRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason, Boolean dormant) {
        reportRepository(descriptor, status, reason, dormant, null, null, null);
    }

    @SneakyThrows
    void reportRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason,
            Boolean dormant, Integer commitCountRaw, Integer contributorCountRaw)
    {
        reportRepository(descriptor, status, reason, dormant, commitCountRaw, contributorCountRaw, null);
    }

    @SneakyThrows
    void reportRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason,
            Boolean dormant, Integer commitCountRaw, Integer contributorCountRaw, String sourceReport)
    {
        repositories.add(descriptor);
        increaseCountByStatus(status);
        if ( status == NcdReportRepositoryReportingStatus.included && Boolean.TRUE.equals(dormant) ) {
            dormantRepositoryCount++;
        }
        writers.repositoryWriter().writeRepository(descriptor, status, reason, dormant, commitCountRaw, contributorCountRaw, sourceReport);
    }

    void reportRepositoryError(INcdReportRepositoryDescriptor descriptor, Exception e) {
        // TODO Log error
        writers.logger().error("Error loading repository: "+descriptor.getUrl(), e);
        reportRepository(descriptor, NcdReportRepositoryReportingStatus.error, e.getMessage());
    }
    
    boolean isPreviouslyReported(INcdReportRepositoryDescriptor descriptor) {
        return repositories.contains(descriptor);
    }
    
    void writeResults() {
        var repositoryCounts = new NcdReportSummaryDescriptor.RepositoryCounts();
        repositoryCounts.setTotal(repositories.size());
        Stream.of(NcdReportRepositoryReportingStatus.values()).forEach(status -> {
            switch ( status ) {
            case included -> repositoryCounts.setIncluded(getCountByStatus(status));
            case excluded -> repositoryCounts.setExcluded(getCountByStatus(status));
            case empty -> repositoryCounts.setEmpty(getCountByStatus(status));
            case error -> repositoryCounts.setError(getCountByStatus(status));
            }
        });
        repositoryCounts.setDormant(dormantRepositoryCount);
        summary.setRepositoryCounts(repositoryCounts);
    }
    
    private void increaseCountByStatus(NcdReportRepositoryReportingStatus status) {
        repositoryCountsByStatus.put(status, getCountByStatus(status)+1);
    }

    private Integer getCountByStatus(NcdReportRepositoryReportingStatus status) {
        return repositoryCountsByStatus.getOrDefault(status, 0);
    }
}
