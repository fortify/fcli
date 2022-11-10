/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.dast_scan.helper.FoDDastScanHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.sast_scan.helper.FoDSastScanHelper;
import com.fortify.cli.fod.sast_scan.helper.FoDSastScanSetupDescriptor;
import com.fortify.cli.fod.sast_scan.helper.FoDStartSastScanRequest;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDInProgressScanActionTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDRemediationScanPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.util.FoDConstants;
import com.fortify.cli.fod.util.FoDEnums;
import com.fortify.cli.fod.util.FoDUtils;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import javax.validation.ValidationException;
import java.io.File;
import java.util.Properties;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Start.CMD_NAME)
public class FoDSastScanStartCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Option(names = {"--purchase-entitlement"})
    private final Boolean purchaseEntitlement = false;
    @Getter
    @Mixin
    private FoDOutputHelperMixins.Create outputHelper;
    @Mixin
    private FoDAppRelResolverMixin.PositionalParameter appRelResolver;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Option(names = {"--notes"})
    private String notes;
    @Option(names = {"--chunk-size"})
    private int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;
    @Option(names = {"--upload-sync-time"})
    private int uploadSyncTime = FoDConstants.DEFAULT_UPLOAD_SYNC_TIME;
    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    @Mixin
    private FoDEntitlementPreferenceTypeOptions.OptionalOption entitlementType;
    @Mixin
    private FoDRemediationScanPreferenceTypeOptions.OptionalOption remediationScanType;

    @Mixin
    private FoDInProgressScanActionTypeOptions.OptionalOption inProgressScanActionType;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        Properties fcliProperties = FoDUtils.loadProperties();

        String relId = appRelResolver.getAppRelId(unirest);
        Integer entitlementIdToUse = 0;

        // get current setup and check if its valid
        FoDSastScanSetupDescriptor currentSetup = FoDSastScanHelper.getSetupDescriptor(unirest, relId);
        if (currentSetup.getTechnologyStack() == null || StringUtils.isEmpty(currentSetup.getTechnologyStack())) {
            throw new ValidationException("The static scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Technology Stack/Language Level' is missing or empty.");
        }

        /**
         * Logic for finding/using "entitlement" and "remediation" scanning is as follows:
         *  - if "entitlement id" is specified directly then use it
         *  - if "remediation" scan specified make sure it is valid and available
         *  - if an "entitlement type" (Static/Subscription) then pass over to API to use
         */
        if (entitlementId != null && entitlementId > 0) {
            entitlementIdToUse = entitlementId;
        }
        if (remediationScanType.getRemediationScanPreferenceType() != null && (remediationScanType.getRemediationScanPreferenceType() == FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
            // if requesting a remediation scan make we have one available
            FoDDastScanHelper.validateRemediationEntitlement(unirest, relId,
                    currentSetup.getEntitlementId(), FoDScanTypeOptions.FoDScanType.Static).getEntitlementId();
        }

        FoDStartSastScanRequest startScanRequest = new FoDStartSastScanRequest()
                .setPurchaseEntitlement(purchaseEntitlement)
                .setEntitlementPreferenceType(remediationScanType.getRemediationScanPreferenceType() != null ?
                        remediationScanType.getRemediationScanPreferenceType().name() : FoDEnums.RemediationScanPreferenceType.NonRemediationScanOnly.name())
                .setInProgressScanActionType(inProgressScanActionType.getInProgressScanActionType() != null ?
                        inProgressScanActionType.getInProgressScanActionType().name() : FoDEnums.InProgressScanActionType.Queue.name())
                .setScanMethodType("Other")
                .setNotes(notes != null && !notes.isEmpty() ? notes : "")
                .setScanTool(fcliProperties.getProperty("projectName", "fcli"))
                .setScanToolVersion(fcliProperties.getProperty("projectVersion", "unknown"));

        if (entitlementId != null && entitlementId > 0) {
            startScanRequest.setEntitlementId(entitlementIdToUse);
        } else if (entitlementType.getEntitlementPreferenceType() != null) {
            startScanRequest.setEntitlementPreferenceType(entitlementType.getEntitlementPreferenceType().name());
        } else {
            throw new ValidationException("Either an 'entitlement id' or 'entitlement type' need to be specified.");
        }

        return FoDSastScanHelper.startScan(unirest, appRelResolver.getAppRelId(unirest), startScanRequest, scanFile,
                chunkSize, uploadSyncTime).asJsonNode();
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
}
