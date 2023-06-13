package com.fortify.cli.util.msp_report.collector;

import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.util.Counter;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting and outputting 
 * {@link MspReportSSCArtifactDescriptor} instances.</p>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class MspReportArtifactCollector {
    private final MspReportResultsWriters writers;
    private final ObjectNode summary;
    
    private Counter totalArtifactsCounter = new Counter();
    private SortedMap<String, Counter> countsByStatus = new TreeMap<>();
    
    @SneakyThrows
    public void report(IUrlConfig urlConfig, MspReportSSCAppVersionDescriptor versionDescriptor, MspReportSSCArtifactDescriptor artifactDescriptor) {
        totalArtifactsCounter.increase();
        increaseCountByProcessingStatus(artifactDescriptor.getStatus());
        writers.artifactsWriter().write(urlConfig, versionDescriptor, artifactDescriptor);
    }

    void writeResults() {
        writeArtifactCounts();
    }

    private void writeArtifactCounts() {
        ObjectNode artifactCounts = JsonHelper.getObjectMapper().createObjectNode();
        artifactCounts.put("total", totalArtifactsCounter.getCount());
        countsByStatus.entrySet().forEach(
            e->artifactCounts.put(PropertyPathFormatter.camelCase(e.getKey()), e.getValue().getCount()));
        summary.set("artifactCounts", artifactCounts);
    }
    
    private void increaseCountByProcessingStatus(String status) {
        getCounterByProcessingStatus(status).increase();
    }

    private Counter getCounterByProcessingStatus(String status) {
        return countsByStatus.computeIfAbsent(status, x->new Counter());
    }
}
