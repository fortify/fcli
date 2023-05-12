package com.fortify.cli.util.msp_report.collector;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.util.Counter;
import com.fortify.cli.util.msp_report.config.MspReportConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportLicenseType;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppSummaryDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor.MspReportSSCArtifactScanType;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import lombok.Data;
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
    private final List<MspReportProcessedArtifactDescriptor> processedArtifacts = new ArrayList<>();
    private final Map<MspReportSSCArtifactScanType, List<MspReportSSCArtifactDescriptor>> applicationLicenseArtifacts = new LinkedHashMap<>();
    
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
        summaryDescriptor.getArtifactsProcessedCounter().increase();
        var licenseType = appDescriptor.getMspLicenseType();
        
        if ( isIgnoredAndContinue(artifactDescriptor) ) {
            return MspReportArtifactCollectorState.CONTINUE; 
        }
        if ( isIgnoredAndDone(licenseType, artifactDescriptor) ) {
            return MspReportArtifactCollectorState.DONE;
        }
        if ( !isBeforeReportingStartDate(artifactDescriptor.getLastScanDate()) ) {
            processWithinReportingPeriod(licenseType, artifactDescriptor);
        } else {
            return processOutsideReportingPeriod(licenseType, artifactDescriptor);
        }
        return MspReportArtifactCollectorState.CONTINUE;
    }
    
    /**
     * This method returns true if we want to ignore the current artifact,
     * and don't need to process any further artifacts for this application
     * version. We're done processing artifacts if license type is Scan
     * or Demo, and upload date is before reporting start date. Note that
     * we need to check upload date instead of scan date as artifacts are
     * ordered by descending upload date, and upload date should always be
     * more recent than scan date. In the following example, we still want 
     * to process artifact #2 even if scan date for #1 is before reporting
     * period start date:
     * <ol>
     *  <li>uploadDate within reporting period, lastScanDate older than 
     *      reporting period start date</li>
     *  <li>Both uploadDate and lastScanDate within reporting period 
     * </ol> 
     */
    private boolean isIgnoredAndDone(MspReportLicenseType licenseType, MspReportSSCArtifactDescriptor artifactDescriptor) {
        var uploadDateTime = artifactDescriptor.getUploadDate();
        return licenseType!=MspReportLicenseType.Application 
                && uploadDateTime!=null 
                && isBeforeReportingStartDate(uploadDateTime) 
                ? true : false;
    }

    /**
     * This method returns true if we want to ignore the current artifact,
     * but want to continue processing further artifacts. We want to ignore 
     * all artifacts that either:
     * <ul>
     *  <li>Have no scan date (usually because there was an error processing 
     *      the artifact, or the artifact needs to be approved)</li>
     *  <li>Are more recent than reporting period end date</li>
     * </ul>
     * Note that we're not ignoring artifacts for which the scan or upload
     * date is before the reporting period start date, as these may still
     * need to be processed for Application license reporting.
     * @return true if we want to ignore the current artifact,
     *         false if we want to process the current artifact
     */
    private boolean isIgnoredAndContinue(MspReportSSCArtifactDescriptor artifactDescriptor) {
        var lastScanDateTime = artifactDescriptor.getLastScanDate();
        return lastScanDateTime==null || isAfterReportingEndDate(lastScanDateTime);
    }

    private void processWithinReportingPeriod(MspReportLicenseType licenseType, MspReportSSCArtifactDescriptor artifactDescriptor) {
        summaryDescriptor.getArtifactsInReportingPeriodCounter().increase();
        switch ( licenseType ) {
        case Application: 
            storeForApplicationLicense(artifactDescriptor);
            break;
        case Demo: 
            storeForDemoOrScanLicense(artifactDescriptor, false, null);
            break;
        case Scan:
            storeForDemoOrScanLicense(artifactDescriptor, true, summaryDescriptor.getConsumedScanEntitlementsCounter());
            break;
        }
    }
    
    private void storeForApplicationLicense(MspReportSSCArtifactDescriptor artifactDescriptor) {
        artifactDescriptor.getScanTypes().forEach(
            type->applicationLicenseArtifacts
                .computeIfAbsent(type, x->new ArrayList<>())
                .add(artifactDescriptor));
    }
    
    private void storeForDemoOrScanLicense(MspReportSSCArtifactDescriptor artifactDescriptor, boolean consumeEntitlementForFortifyScans, Counter counter) {
        artifactDescriptor.getScanTypes()
            .forEach(type->store(
                    artifactDescriptor, type, 
                    consumeEntitlementForFortifyScans,
                    artifactDescriptor.getLastScanDate(),
                    counter));
    }
    
    private void store(MspReportSSCArtifactDescriptor artifactDescriptor, MspReportSSCArtifactScanType entitlementScanType, boolean consumeEntitlementForFortifyScans, LocalDateTime entitlementConsumptionDate, Counter counter) {
        var entitlementConsumed = entitlementScanType.isFortifyScan() && consumeEntitlementForFortifyScans;
        if ( !entitlementConsumed ) {
            entitlementConsumptionDate = null;
        } else {
            counter.increase();
        }
        processedArtifacts.add(new MspReportProcessedArtifactDescriptor(
                artifactDescriptor,
                entitlementScanType,
                entitlementConsumed, 
                entitlementConsumptionDate));
    }

    private MspReportArtifactCollectorState processOutsideReportingPeriod(MspReportLicenseType licenseType, MspReportSSCArtifactDescriptor artifactDescriptor) {
        if ( licenseType!=MspReportLicenseType.Application ) {
            // We're only interested in the artifact if license type is Application,
            // however this method may be invoked if uploadDate is within reporting 
            // period but lastScanDate is not.
            return MspReportArtifactCollectorState.CONTINUE;
        }
        var matchingScanTypes = artifactDescriptor.getMatchingFortifyScanTypes(applicationLicenseArtifacts.keySet());
        for ( var matchingScanType : matchingScanTypes ) {
            applicationLicenseArtifacts.get(matchingScanType)
                .forEach(matchingArtifact->store(
                        matchingArtifact, 
                        matchingScanType, 
                        false, 
                        artifactDescriptor.getLastScanDate(),
                        summaryDescriptor.getConsumedApplicationEntitlementsCounter()));
        }
        matchingScanTypes.forEach(applicationLicenseArtifacts::remove);
        return matchingScanTypes.isEmpty() && !applicationLicenseArtifacts.isEmpty()
            ? MspReportArtifactCollectorState.CONTINUE
            : MspReportArtifactCollectorState.DONE;
    }
    
    private boolean isAfterReportingEndDate(LocalDateTime dateTime) {
        var reportingEndDateTime = reportConfig.getReportingEndDate().atTime(LocalTime.MAX);
        return dateTime.isAfter(reportingEndDateTime);
    }
    
    private boolean isBeforeReportingStartDate(LocalDateTime dateTime) {
        var reportingStartDateTime = reportConfig.getReportingStartDate().atStartOfDay();
        return dateTime.isBefore(reportingStartDateTime);
    }

    public void close() {
        storeRemainingArtifacts();
        processedArtifacts.forEach(this::writeResult);
    }
    
    private void storeRemainingArtifacts() {
        applicationLicenseArtifacts.entrySet().forEach(this::storeRemainingArtifacts);
    }
    
    private void storeRemainingArtifacts(Map.Entry<MspReportSSCArtifactScanType, List<MspReportSSCArtifactDescriptor>> entry) {
        var entitlementScanType = entry.getKey();
        var descriptors = entry.getValue();
        var size = descriptors.size();
        var entitlementConsumedDate = descriptors.get(size-1).getLastScanDate();
        for ( int i = 0 ; i < size ; i++ ) {
            var artifactDescriptor = descriptors.get(i);
            store(artifactDescriptor, 
                    entitlementScanType, 
                    i==size-1, 
                    entitlementConsumedDate,
                    summaryDescriptor.getConsumedApplicationEntitlementsCounter());
        }
    }
    
    private void writeResult(MspReportProcessedArtifactDescriptor descriptor) {
        writers.artifactsWriter().write(urlConfig, appDescriptor, descriptor.artifactDescriptor, descriptor.entitlementScanType, descriptor.entitlementConsumed, descriptor.entitlementConsumptionDate);
    }
    
    public static enum MspReportArtifactCollectorState {
        CONTINUE, DONE
    }
    
    @Data
    public static final class MspReportProcessedArtifactDescriptor {
        private final MspReportSSCArtifactDescriptor artifactDescriptor;
        private final MspReportSSCArtifactScanType entitlementScanType;
        private final boolean entitlementConsumed;
        private final LocalDateTime entitlementConsumptionDate;
    }
}
