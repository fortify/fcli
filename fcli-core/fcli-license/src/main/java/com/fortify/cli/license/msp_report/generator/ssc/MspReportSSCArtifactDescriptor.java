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
package com.fortify.cli.license.msp_report.generator.ssc;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes an SSC artifact with embedded scans. 
 */
@Reflectable @NoArgsConstructor 
@Data @EqualsAndHashCode(callSuper = false)
public final class MspReportSSCArtifactDescriptor extends JsonNodeHolder {
    private String id;
    private String originalFileName;
    private LocalDateTime uploadDate;
    private boolean purged;
    private MspReportSSCArtifactEmbedDescriptor _embed;
    // For now, we just write the status to the output. If we ever want to add any
    // status-based logic, we should use a proper enum.
    private String status;
    
    public void setUploadDate(ZonedDateTime zonedDateTime) {
        this.uploadDate = zonedDateTime==null ? null : zonedDateTime.toLocalDateTime();
    }
    
    public final MspReportSSCArtifactScanDescriptor[] getScans() {
        // There may not be any scans if the artifact wasn't successfully processed,
        // for example if it requires approval or if there was an error processing.
        MspReportSSCArtifactScanDescriptor[] result = _embed==null ? null : _embed.getScans();
        return result!=null ? result : new MspReportSSCArtifactScanDescriptor[] {};
    }
    
    public final ObjectNode updateReportRecord(ObjectNode record) {
        return record.put("artifactId", id)
            .put("originalFileName", originalFileName)
            .put("uploadDate", toString(uploadDate))
            .put("purged", purged)
            .put("status", status)
            .put("scanTypes", getScanTypes());
    }

    private String toString(LocalDateTime localDateTime) {
        return localDateTime==null ? "N/A" : localDateTime.toString();
    }

    public final boolean hasScans() {
        return getScans().length>0;
    }
    
    private final String getScanTypes() {
        return Stream.of(getScans())
                .map(MspReportSSCArtifactScanDescriptor::getType)
                .map(MspReportSSCScanType::toString)
                .collect(Collectors.joining("+"));
    }
    
    @Reflectable @NoArgsConstructor 
    @Data @EqualsAndHashCode(callSuper = false)
    private static final class MspReportSSCArtifactEmbedDescriptor {
        private MspReportSSCArtifactScanDescriptor[] scans;
    }
        
    @Reflectable @NoArgsConstructor 
    @Data @EqualsAndHashCode(callSuper = false)
    public static final class MspReportSSCArtifactScanDescriptor extends JsonNodeHolder {
        private String id;
        private LocalDateTime scanDate;
        private MspReportSSCScanType type;
        
        // The SSC REST API response includes a field named uploadDate,
        // but usually this is actually the scan date so we store this
        // in a scanDate property.
        public void setUploadDate(ZonedDateTime zonedDateTime) {
            this.scanDate = zonedDateTime==null ? null : zonedDateTime.toLocalDateTime();
        }
        
        public void setType(String type) {
            if ( StringUtils.isBlank(type) ) {
                this.type = MspReportSSCScanType.OTHER;
            } else {
                switch (type.toUpperCase()) {
                case "SCA": this.type = MspReportSSCScanType.SAST; break;
                case "SECURITYSCOPE": this.type = MspReportSSCScanType.RUNTIME; break; 
                case "WEBINSPECT": this.type = MspReportSSCScanType.DAST; break; 
                default: this.type = MspReportSSCScanType.OTHER; break; 
                }
            }
        }
    }
}
