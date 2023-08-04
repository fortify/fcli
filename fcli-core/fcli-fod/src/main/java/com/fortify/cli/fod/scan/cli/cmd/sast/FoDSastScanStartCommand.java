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

package com.fortify.cli.fod.scan.cli.cmd.sast;

import java.io.File;
import java.util.Properties;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.util.FoDConstants;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDInProgressScanActionTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDRemediationScanPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.sast.FoDSastScanHelper;
import com.fortify.cli.fod.scan.helper.sast.FoDSastScanSetupDescriptor;
import com.fortify.cli.fod.scan.helper.sast.FoDStartSastScanRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.StartSast.CMD_NAME)
public class FoDSastScanStartCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.StartSast outputHelper;
    
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Option(names = {"--purchase-entitlement"})
    private final Boolean purchaseEntitlement = false;
    @Option(names = {"--notes"})
    private String notes;
    @Option(names = {"--chunk-size"})
    private int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;
    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    @Mixin
    private FoDEntitlementPreferenceTypeOptions.OptionalOption entitlementType;
    @Mixin
    private FoDRemediationScanPreferenceTypeOptions.OptionalOption remediationScanType;
    @Mixin
    private FoDInProgressScanActionTypeOptions.OptionalOption inProgressScanActionType;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO: refactor use of FoDEnums and use @JsonValues as per: https://github.com/fortify/fcli/issues/279
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            String relId = releaseDescriptor.getReleaseId();
            
            // get current setup and check if its valid
            FoDSastScanSetupDescriptor currentSetup = FoDSastScanHelper.getSetupDescriptor(unirest, relId);
            if (StringUtils.isBlank(currentSetup.getTechnologyStack())) {
                throw new ValidationException("The static scan configuration for release with id '" + relId +
                        "' has not been setup correctly - 'Technology Stack/Language Level' is missing or empty.");
            }

            // get entitlement to use
            FoDAssessmentTypeDescriptor entitlementToUse = getEntitlementToUse(unirest, progressWriter, relId, currentSetup);

            FoDStartSastScanRequest startScanRequest = FoDStartSastScanRequest.builder()
                    .purchaseEntitlement(purchaseEntitlement)
                    .inProgressScanActionType(inProgressScanActionType.getInProgressScanActionType() != null ?
                            inProgressScanActionType.getInProgressScanActionType().name() : FoDEnums.InProgressScanActionType.Queue.name())
                    .scanMethodType("Other")
                    .notes(notes != null && !notes.isEmpty() ? notes : "")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown"))
                    .build();

            if (entitlementId != null && entitlementId > 0) {
                startScanRequest.setEntitlementId(entitlementToUse.getEntitlementId());
            } else if (entitlementType.getEntitlementPreferenceType() != null) {
                startScanRequest.setEntitlementPreferenceType(entitlementType.getEntitlementPreferenceType().name());
            } else {
                startScanRequest.setEntitlementPreferenceType(String.valueOf(FoDEnums.EntitlementPreferenceType.SubscriptionFirstThenSingleScan));
            }

            return FoDSastScanHelper.startScan(unirest, releaseDescriptor, startScanRequest, scanFile, chunkSize).asJsonNode();
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "STARTED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    private FoDAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId, FoDSastScanSetupDescriptor currentSetup) {
        FoDAssessmentTypeDescriptor entitlementToUse = new FoDAssessmentTypeDescriptor();

        /**
         * Logic for finding/using "entitlement" and "remediation" scanning is as follows:
         *  - if "entitlement id" is specified directly then use it
         *  - if "remediation" scan specified make sure it is valid and available
         *  - if an "entitlement type" (Single/Subscription) then pass to the API to use
         */
        if (entitlementId != null && entitlementId > 0) {
            entitlementToUse.setEntitlementId(entitlementId);
        }
        if (remediationScanType.getRemediationScanPreferenceType() != null && (remediationScanType.getRemediationScanPreferenceType() == FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
            // if requesting a remediation scan make we have one available
            FoDSastScanHelper.validateRemediationEntitlement(unirest, progressWriter, relId,
                    currentSetup.getEntitlementId(), FoDScanTypeOptions.FoDScanType.Static).getEntitlementId();
        }
        return entitlementToUse;
    }
}
