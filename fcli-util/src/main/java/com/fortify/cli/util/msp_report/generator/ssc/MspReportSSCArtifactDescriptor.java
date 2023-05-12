package com.fortify.cli.util.msp_report.generator.ssc;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public final class MspReportSSCArtifactDescriptor extends JsonNodeHolder {
    private String id;
    private LocalDateTime lastScanDate;
    private LocalDateTime uploadDate;
    private boolean purged;
    private MspReportSSCArtifactScanStatus scaStatus;
    private MspReportSSCArtifactScanStatus webInspectStatus;
    private MspReportSSCArtifactScanStatus runtimeStatus;
    private MspReportSSCArtifactScanStatus otherStatus;
    @JsonIgnore private Set<MspReportSSCArtifactScanType> scanTypes;
    @JsonIgnore private Set<MspReportSSCArtifactScanType> fortifyScanTypes;
    
    public void setLastScanDate(ZonedDateTime zonedDateTime) {
        this.lastScanDate = zonedDateTime==null ? null : zonedDateTime.toLocalDateTime();
    }
    
    public void setUploadDate(ZonedDateTime zonedDateTime) {
        this.uploadDate = zonedDateTime==null ? null : zonedDateTime.toLocalDateTime();
    }
    
    public boolean isFortifySastScan() {
        return hasScan(scaStatus);
    }
    
    public boolean isFortifyDastScan() {
        return hasScan(webInspectStatus);
    }
    
    public boolean isFortifyRuntimeScan() {
        return hasScan(runtimeStatus);
    }
    
    public boolean isOtherScan() {
        return hasScan(otherStatus);
    }
    
    public Set<MspReportSSCArtifactScanType> getScanTypes() {
        if ( scanTypes==null ) {
            scanTypes = new HashSet<>(4);
            if ( isFortifySastScan() ) { scanTypes.add(MspReportSSCArtifactScanType.SAST); }
            if ( isFortifyDastScan() ) { scanTypes.add(MspReportSSCArtifactScanType.DAST); }
            if ( isFortifyRuntimeScan() ) { scanTypes.add(MspReportSSCArtifactScanType.RUNTIME); }
            if ( isOtherScan() ) { scanTypes.add(MspReportSSCArtifactScanType.OTHER); }
        }
        return scanTypes;
    }
    
    public Set<MspReportSSCArtifactScanType> getMatchingFortifyScanTypes(Collection<MspReportSSCArtifactScanType> scanTypes) {
        var result = new HashSet<>(getFortifyScanTypes());
        result.retainAll(scanTypes);
        return result;
    }
    
    public String getScanTypesString() {
        var scanTypes = getScanTypes();
        return scanTypes.isEmpty() 
                ? "N/A" 
                : scanTypes.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining("+"));
    }
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode
                .put("artifactId", id)
                .put("scanType", getScanTypesString())
                .put("uploadDate", toString(uploadDate))
                .put("lastScanDate", toString(lastScanDate))
                .put("purged", purged);
    }
    
    private String toString(LocalDateTime zonedDateTime) {
        return zonedDateTime==null ? "N/A" : zonedDateTime.toString();
    }

    public static final boolean hasScan(MspReportSSCArtifactScanStatus status) {
        return status==MspReportSSCArtifactScanStatus.IGNORED 
                || status==MspReportSSCArtifactScanStatus.PROCESSED;
    }
    
    public static enum MspReportSSCArtifactScanStatus {
        NONE, NOT_EXIST, IGNORED, PROCESSED;
    }
    
    @RequiredArgsConstructor
    public static enum MspReportSSCArtifactScanType {
        SAST(true), DAST(true), RUNTIME(true), OTHER(false);
        
        @Getter private final boolean fortifyScan;
    }
}
