package com.fortify.cli.util.msp_report.generator.ssc;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor.MspReportSSCArtifactScanDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class describes a single scan, together with some data from the artifact that 
 * contains the scan.
 * @author rsenden
 *
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public final class MspReportSSCScanDescriptor extends JsonNodeHolder {
    private final String artifactId;
    private final LocalDateTime artifactUploadDate;
    private final boolean artifactPurged;
    private final String scanId;
    private final LocalDateTime scanDate;
    private final MspReportSSCScanType scanType;
    
    private MspReportSSCScanDescriptor(MspReportSSCArtifactDescriptor artifactDescriptor, MspReportSSCArtifactScanDescriptor artifactScanDescriptor) {
        this.artifactId = artifactDescriptor.getId();
        this.artifactUploadDate = artifactDescriptor.getUploadDate();
        this.artifactPurged = artifactDescriptor.isPurged();
        this.scanId = artifactScanDescriptor.getId();
        this.scanDate = artifactScanDescriptor.getScanDate();
        this.scanType = artifactScanDescriptor.getType();
    }
    
    public static final Stream<MspReportSSCScanDescriptor> from(MspReportSSCArtifactDescriptor artifactDescriptor) {
        return Stream.of(artifactDescriptor.getScans())
            .map(artifactScanDescriptor->new MspReportSSCScanDescriptor(artifactDescriptor, artifactScanDescriptor));
    }
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode
                .put("artifactId", artifactId)
                .put("uploadDate", toString(artifactUploadDate))
                .put("scanId", scanId)
                .put("scanType", getScanType().toString())
                .put("scanDate", toString(scanDate))
                .put("purged", artifactPurged);
    }
    
    private String toString(LocalDateTime localDateTime) {
        return localDateTime==null ? "N/A" : localDateTime.toString();
    }
}
