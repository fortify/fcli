package com.fortify.cli.util.msp_report.collector;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.config.MspReportConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportLicenseType;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppSummaryDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor.MspReportSSCArtifactScanType;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import lombok.Data;
import lombok.Getter;
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
public final class MspReportAppArtifactCollector implements AutoCloseable {
    private final MspReportConfig reportConfig;
    private final MspReportResultsWriters writers;
    private final IUrlConfig urlConfig;
    private final MspReportSSCAppDescriptor appDescriptor;
    private final MspReportSSCAppSummaryDescriptor summaryDescriptor = new MspReportSSCAppSummaryDescriptor();
    private final List<MspReportSSCArtifactDescriptor> processedArtifacts = new ArrayList<>();
    private final Map<MspReportSSCArtifactScanType, MspReportSSCArtifactDescriptor> earliestArtifactByScanType = new HashMap<>();
    
    public MspReportSSCAppSummaryDescriptor summary() {
        return summaryDescriptor;
    }
    
    /**
     * Report an SSC application version artifact. 
     * @return {@link MspReportArtifactCollectorState#DONE} is we're done processing
     *         this application version, {@link MspReportArtifactCollectorState#DONE}
     *         otherwise.
     */
    @SneakyThrows
    public MspReportArtifactCollectorState report(MspReportSSCArtifactDescriptor artifactDescriptor) {
        processedArtifacts.add(artifactDescriptor);
        addEarliestArtifact(artifactDescriptor);
        return continueOrDone(artifactDescriptor);
    }
    
    /**
     * This method returns DONE if we don't need to process any further artifacts 
     * for this application version. We're done processing artifacts if:
     * <ul>
     *  <li>Upload date is before contract start date.</li>
     *  <li>License type is Scan or Demo, and upload date is before reporting 
     *      start date.</li>
     * <ul>
     * Note that we need to check upload date instead of scan date as artifacts 
     * are ordered by descending upload date, and upload date should always be
     * more recent than scan date. In the following example, we still want to 
     * process artifact #2 even if scan date for #1 is before reporting period 
     * start date:
     * <ol>
     *  <li>uploadDate within reporting period, lastScanDate older than 
     *      reporting period start date</li>
     *  <li>Both uploadDate and lastScanDate within reporting period 
     * </ol> 
     */
    private MspReportArtifactCollectorState continueOrDone(MspReportSSCArtifactDescriptor artifactDescriptor) {
        var licenseType = appDescriptor.getMspLicenseType();
        var uploadDateTime = artifactDescriptor.getUploadDate();
        if ( isBeforeContractStartDate(uploadDateTime) ) { 
            return MspReportArtifactCollectorState.DONE; 
        }
        else if ( licenseType!=MspReportLicenseType.Application && isBeforeReportingStartDate(uploadDateTime) ) {
            return MspReportArtifactCollectorState.DONE;
        }
        return MspReportArtifactCollectorState.CONTINUE;
    }
    
    private boolean isAfterReportingEndDate(LocalDateTime dateTime) {
        var reportingEndDateTime = reportConfig.getReportingEndDate().atTime(LocalTime.MAX);
        return dateTime.isAfter(reportingEndDateTime);
    }
    
    private boolean isBeforeReportingStartDate(LocalDateTime dateTime) {
        var reportingStartDateTime = reportConfig.getReportingStartDate().atStartOfDay();
        return dateTime.isBefore(reportingStartDateTime);
    }
    
    private boolean isBeforeContractStartDate(LocalDateTime dateTime) {
        var reportingStartDateTime = reportConfig.getContractStartDate().atStartOfDay();
        return dateTime.isBefore(reportingStartDateTime);
    }
    
    private void addEarliestArtifact(MspReportSSCArtifactDescriptor artifactDescriptor) {
        artifactDescriptor.getFortifyScanTypes().forEach(
                type->addEarliestArtifact(type, artifactDescriptor));
    }
    
    private void addEarliestArtifact(MspReportSSCArtifactScanType type, MspReportSSCArtifactDescriptor artifactDescriptor) {
        var previousEarliestArtifactDescriptor = earliestArtifactByScanType.get(type);
        if ( previousEarliestArtifactDescriptor==null || previousEarliestArtifactDescriptor.getLastScanDate().isAfter(artifactDescriptor.getLastScanDate()) ) {
           earliestArtifactByScanType.put(type, artifactDescriptor);
        }
    }

    public void close() {
        processedArtifacts.stream()
            .map(this::getProcessedArtifactDescriptors)
            .flatMap(List::stream)
            .forEach(this::write);
    }
    
    private void write(MspReportProcessedArtifactDescriptor descriptor) {
        var writer = writers.processedArtifactsWriter();
        summaryDescriptor.getArtifactsProcessedCounter().increase();
        writer.writeProcessed(descriptor);
        if ( !descriptor.getEntitlementConsumedReason().isOutsideReportingPeriod() ) {
            summaryDescriptor.getArtifactsInReportingPeriodCounter().increase();
            writer.writeInReportingPeriod(descriptor);
        }
        if ( descriptor.getEntitlementConsumed()==MspReportArtifactEntitlementConsumed.application ) {
            summaryDescriptor.getConsumedApplicationEntitlementsCounter().increase();
            writer.writeEntitlementConsuming(descriptor);
        }
        if ( descriptor.getEntitlementConsumed()==MspReportArtifactEntitlementConsumed.scan ) {
            summaryDescriptor.getConsumedScanEntitlementsCounter().increase();
            writer.writeEntitlementConsuming(descriptor);
        }
    }
    
    private List<MspReportProcessedArtifactDescriptor> getProcessedArtifactDescriptors(MspReportSSCArtifactDescriptor descriptor) {
        return descriptor.getScanTypes().stream()
            .map(type->getProcessedArtifactDescriptor(descriptor, type))
            .collect(Collectors.toList());
    }
   
    private MspReportProcessedArtifactDescriptor getProcessedArtifactDescriptor(MspReportSSCArtifactDescriptor descriptor, MspReportSSCArtifactScanType type) {
        var entitlementConsumedReason = getEntitlementConsumedReason(descriptor, type);
        var entitlementConsumptionDate = getEntitlementConsumptionDate(descriptor, type, entitlementConsumedReason);
        return new MspReportProcessedArtifactDescriptor(urlConfig, appDescriptor, descriptor, type, entitlementConsumedReason, entitlementConsumptionDate);
    }
    
    private MspReportArtifactEntitlementConsumedReason getEntitlementConsumedReason(MspReportSSCArtifactDescriptor descriptor, MspReportSSCArtifactScanType type) {
        var lastScanDate = descriptor.getLastScanDate();
        
        if ( isBeforeContractStartDate(lastScanDate) ) {
            return MspReportArtifactEntitlementConsumedReason.beforeContractStartDate;
        } else if ( isBeforeReportingStartDate(lastScanDate) ) {
            return MspReportArtifactEntitlementConsumedReason.beforeReportingPeriod;
        } else if ( isAfterReportingEndDate(lastScanDate) ) {
            return MspReportArtifactEntitlementConsumedReason.afterReportingPeriod;
        } 
        if ( !type.isFortifyScan() ) {
            return MspReportArtifactEntitlementConsumedReason.noFortifyScan;
        } else {
            switch ( appDescriptor.getMspLicenseType() ) {
            case Demo: 
                return MspReportArtifactEntitlementConsumedReason.demoScan;
            case Scan: 
                return MspReportArtifactEntitlementConsumedReason.mspScanLicenseConsumed;
            case Application:
                if ( descriptor.equals(earliestArtifactByScanType.get(type)) ) {
                    return MspReportArtifactEntitlementConsumedReason.mspApplicationLicenseConsumed;
                } else {
                    return MspReportArtifactEntitlementConsumedReason.mspApplicationLicenseConsumedEarlier;
                }
            default: throw new RuntimeException("Unable to determine entitlement consumed reason");
            }
        }
    }

    private LocalDateTime getEntitlementConsumptionDate(MspReportSSCArtifactDescriptor descriptor, MspReportSSCArtifactScanType type, MspReportArtifactEntitlementConsumedReason entitlementConsumedReason) {
        var isApplicationLicense = appDescriptor.getMspLicenseType()==MspReportLicenseType.Application;
        switch ( entitlementConsumedReason.getEntitlementConsumed() ) {
        case none:
            if ( !isApplicationLicense || !type.isFortifyScan() ) { return null; }
            // Intentionally no break;
        case application:
            return earliestArtifactByScanType.get(type).getLastScanDate();
        case scan:
            return descriptor.getLastScanDate();
        default: throw new RuntimeException("Unable to determine entitlement consumed date");
        }
    }


    public static enum MspReportArtifactCollectorState {
        CONTINUE, DONE
    }
    
    @RequiredArgsConstructor
    public static enum MspReportArtifactEntitlementConsumedReason {
        beforeContractStartDate(MspReportArtifactEntitlementConsumed.none, true),
        beforeReportingPeriod(MspReportArtifactEntitlementConsumed.none, true),
        afterReportingPeriod(MspReportArtifactEntitlementConsumed.none, true),
        noFortifyScan(MspReportArtifactEntitlementConsumed.none, false),
        demoScan(MspReportArtifactEntitlementConsumed.none, false),
        mspScanLicenseConsumed(MspReportArtifactEntitlementConsumed.scan, false),
        mspApplicationLicenseConsumed(MspReportArtifactEntitlementConsumed.application, false),
        mspApplicationLicenseConsumedEarlier(MspReportArtifactEntitlementConsumed.none, false),
        ;
        
        @Getter private final MspReportArtifactEntitlementConsumed entitlementConsumed;
        @Getter private final boolean outsideReportingPeriod;
    }
    
    public static enum MspReportArtifactEntitlementConsumed {
        scan, application, none
    }
    
    @Data
    public static final class MspReportProcessedArtifactDescriptor {
        private final IUrlConfig urlConfig; 
        private final MspReportSSCAppDescriptor appDescriptor;
        private final MspReportSSCArtifactDescriptor artifactDescriptor;
        private final MspReportSSCArtifactScanType entitlementScanType;
        private final MspReportArtifactEntitlementConsumedReason entitlementConsumedReason;
        private final LocalDateTime entitlementConsumptionDate;
        private ObjectNode reportNode;
        
        public MspReportArtifactEntitlementConsumed getEntitlementConsumed() {
            return entitlementConsumedReason.getEntitlementConsumed();
        }
        
        public ObjectNode getReportNode() {
            if ( reportNode==null ) {
                var entitlementConsumedReason = getEntitlementConsumedReason();
                var entitlementConsumed = getEntitlementConsumed();
                var entitlementConsumptionDate = getEntitlementConsumptionDate();
                reportNode = 
                    getArtifactDescriptor().updateReportRecord(
                        getAppDescriptor().updateReportRecord(
                                JsonHelper.getObjectMapper().createObjectNode()
                                    .put("url", getUrlConfig().getUrl())))
                    .put("entitlementScanType", getEntitlementScanType().name())
                    .put("entitlementConsumed", entitlementConsumed.name())
                    .put("entitlementConsumedReason", entitlementConsumedReason.name())
                    .put("entitlementConsumptionDate", entitlementConsumptionDate==null?"N/A":entitlementConsumptionDate.toString());
            }
            return reportNode;
        }
    }
}
