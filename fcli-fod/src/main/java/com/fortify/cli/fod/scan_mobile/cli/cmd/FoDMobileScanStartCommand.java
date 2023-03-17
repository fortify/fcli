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

package com.fortify.cli.fod.scan_mobile.cli.cmd;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.fod.lookup.cli.mixin.FoDLookupTypeOptions;
import com.fortify.cli.fod.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanFormatOptions;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan_mobile.helper.FoDMobileScanHelper;
import com.fortify.cli.fod.scan_mobile.helper.FoDStartMobileScanRequest;
import com.fortify.cli.fod.util.FoDConstants;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.StartMobile.CMD_NAME)
public class FoDMobileScanStartCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    @Getter @Mixin private FoDOutputHelperMixins.StartMobile outputHelper;
    @Mixin
    private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    private enum MobileFrameworks { iOS, Android }
    @Option(names = {"--framework"}, required = true)
    private MobileFrameworks mobileFramework;
    @Option(names = {"--timezone"})
    private String timezone;
    @Option(names = {"--start-date"})
    private String startDate;
    @Option(names = {"--notes"})
    private String notes;
    @Option(names = {"--chunk-size"})
    private int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;
    @Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    @Mixin
    private FoDEntitlementPreferenceTypeOptions.OptionalOption entitlementType;
    @Mixin
    private FoDAssessmentTypeOptions.OptionalOption assessmentType;

    // TODO Split into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
        FoDAssessmentTypeDescriptor entitlementToUse = new FoDAssessmentTypeDescriptor();

        String relId = appMicroserviceRelResolver.getAppMicroserviceRelId(unirest);

        // TODO: should we check if scan is already running ?

        /**
         * Logic for finding/using "entitlement" and "remediation" scanning is as follows:
         *  - if "entitlement id" is specified directly then use it
         *  - if an "assessment type" (Mobile/Mobile+) and "entitlement type" (Static/Subscription) then find an
         *    appropriate entitlement to use
         *  - otherwise fail
         */
        if (entitlementId != null && entitlementId > 0) {
            //entitlementToUse.copyFromCurrentSetup(currentSetup);
            entitlementToUse.setEntitlementId(entitlementId);
        } else if (assessmentType.getAssessmentType() != null && entitlementType.getEntitlementPreferenceType() != null) {
            // if assessment and entitlement type are both specified, find entitlement to use
            entitlementToUse = FoDMobileScanHelper.getEntitlementToUse(unirest, relId,
                    assessmentType.getAssessmentType(), entitlementType.getEntitlementPreferenceType(),
                    FoDScanFormatOptions.FoDScanType.Mobile);
        } else {
            throw new ValidationException("Please specify an 'entitlement id' or an 'entitlement preference' and 'assessment type'.");
        }

        if (entitlementToUse.getEntitlementId() == null || entitlementToUse.getEntitlementId() <= 0) {
            throw new ValidationException("Could not find a valid FoD entitlement to use.");
        }

        // find/check timeZone if specified
        if (timezone != null && !timezone.isEmpty()) {
            try {
                FoDLookupDescriptor lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.TimeZones, timezone, true);
            } catch (JsonProcessingException ex) {
                throw new ValidationException(ex.getMessage());
            }
        // else default to UTC
        } else {
            timezone = "UTC";
        }

        String startDateStr = (startDate == null || startDate.isEmpty())
                ? LocalDateTime.now().format(dtf)
                : LocalDateTime.parse(startDate, dtf).toString();

        FoDStartMobileScanRequest startScanRequest = new FoDStartMobileScanRequest()
                .setStartDate(startDateStr)
                .setAssessmentTypeId(entitlementToUse.getAssessmentTypeId())
                .setEntitlementId(entitlementToUse.getEntitlementId())
                .setEntitlementFrequencyType(entitlementToUse.getFrequencyType())
                .setTimeZone(timezone)
                .setFrameworkType(mobileFramework.name())
                .setScanMethodType("Other")
                .setNotes(notes != null && !notes.isEmpty() ? notes : "")
                .setScanTool(fcliProperties.getProperty("projectName", "fcli"))
                .setScanToolVersion(fcliProperties.getProperty("projectVersion", "unknown"));

        return FoDMobileScanHelper.startScan(unirest, relId, startScanRequest, scanFile, chunkSize).asJsonNode();
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
