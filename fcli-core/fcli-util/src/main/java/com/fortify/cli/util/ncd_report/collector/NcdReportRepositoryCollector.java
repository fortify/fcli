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
package com.fortify.cli.util.ncd_report.collector;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.util.ncd_report.writer.NcdReportRepositoriesWriter.NcdReportRepositoryReportingStatus;
import com.fortify.cli.util.ncd_report.writer.NcdReportResultsWriters;

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
    private final ObjectNode summary;
    
    private Set<INcdReportRepositoryDescriptor> repositories = new LinkedHashSet<>();
    private Map<NcdReportRepositoryReportingStatus, Integer> repositoryCountsByStatus = new HashMap<>();
    
    @SneakyThrows
    void reportRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason) {
        repositories.add(descriptor);
        increaseCountByStatus(status);
        writers.repositoryWriter().writeRepository(descriptor, status, reason);
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
        ObjectNode repositoryCounts = JsonHelper.getObjectMapper().createObjectNode();
        repositoryCounts.put("total", repositories.size());
        Stream.of(NcdReportRepositoryReportingStatus.values())
            .forEach(status->repositoryCounts.put(status.name(), getCountByStatus(status)));
        summary.set("repositoryCounts", repositoryCounts);
    }
    
    private void increaseCountByStatus(NcdReportRepositoryReportingStatus status) {
        repositoryCountsByStatus.put(status, getCountByStatus(status)+1);
    }

    private Integer getCountByStatus(NcdReportRepositoryReportingStatus status) {
        return repositoryCountsByStatus.getOrDefault(status, 0);
    }
}
