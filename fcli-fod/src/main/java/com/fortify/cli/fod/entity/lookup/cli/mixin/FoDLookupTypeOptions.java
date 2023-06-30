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
package com.fortify.cli.fod.entity.lookup.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDLookupTypeOptions {
    public enum FoDLookupType {
        All,
        MobileScanPlatformTypes,
        MobileScanFrameworkTypes,
        MobileScanEnvironmentTypes,
        MobileScanRoleTypes,
        MobileScanExternalDeviceTypes,
        DynamicScanEnvironmentFacingTypes,
        DynamicScanAuthenticationTypes,
        TimeZones,
        RepeatScheduleTypes,
        GeoLocations,
        SDLCStatusTypes,
        DayOfWeekTypes,
        BusinessCriticalityTypes,
        ReportTemplateTypes,
        AnalysisStatusTypes,
        ScanStatusTypes,
        ReportFormats,
        Roles,
        ScanPreferenceTypes,
        AuditPreferenceTypes,
        EntitlementFrequencyTypes,
        ApplicationTypes,
        ScanTypes,
        AttributeTypes,
        AttributeDataTypes,
        MultiFactorAuthorizationTypes,
        ReportTypes,
        ReportStatusTypes,
        PassFailReasonTypes,
        DynamicScanWebServiceTypes,
        VulnerabilitySeverityTypes,
        TechnologyTypes,
        LanguageLevels,
        AuditActionTypes,
        NotificationTriggerTypes,
        ConcurrentRequestThreadsTypes,
        MobileScanAuditPreferenceTypes,
        DataExportTypes,
        ScanMethodTypes,
        StartScanMethodTypes,
        AuditTemplateConditionTypes,
        OpenSourceScanTypeAuditTemplateFieldTypes,
        StaticScanTypeAuditTemplateFieldTypes,
        DynamicMobileScanTypeAuditTemplateFieldTypes
    }

    public static final class FoDLookupTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDLookupTypeIterable() {
            super(Stream.of(FoDLookupType.values()).map(FoDLookupType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDLookupType {
        public abstract FoDLookupType getLookupType();
    }

    public static class RequiredLookupOption extends AbstractFoDLookupType {
        @Option(names = {"--type", "--lookup-type"}, required = true, defaultValue = "All", completionCandidates = FoDLookupTypeIterable.class)
        @Getter private FoDLookupType lookupType;
    }

    public static class OptionalLookupOption extends AbstractFoDLookupType {
        @Option(names = {"--type", "--lookup-type"}, required = false, defaultValue = "All", completionCandidates = FoDLookupTypeIterable.class)
        @Getter private FoDLookupType lookupType;
    }

}
