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

package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.dast_scan.helper.FoDDastScanHelper;
import com.fortify.cli.fod.dast_scan.helper.FoDDastScanSetupDescriptor;
import com.fortify.cli.fod.dast_scan.helper.FoDStartDastScanRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppRelResolverMixin;
import com.fortify.cli.fod.release.helper.FoDAppRelDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Start.CMD_NAME)
public class FoDDastScanStartCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    @Mixin private FoDAppRelResolverMixin.PositionalParameter appRelResolver;

    @Option(names = {"--start-date"})
    private String startDate;

    @Option(names = {"--remediation"})
    private Boolean remediation = false;

    @Option(names = {"--skip-if-running"})
    private Boolean skipIfRunning = false;

    @Mixin private FoDEntitlementTypeOptions.OptionalOption entitlementType;
    @Mixin private FoDAssessmentTypeOptions.OptionalOption assessmentType;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        Properties fcliProperties = FoDScanHelper.loadProperties();
        FoDAssessmentTypeDescriptor entitlementToUse = new FoDAssessmentTypeDescriptor();

        String relId = appRelResolver.getAppRelId(unirest);
        System.out.println(relId);

        // check if scan is already running
        FoDAppRelDescriptor appRelDescriptor = FoDAppRelHelper.getAppRelDescriptorById(unirest, relId, true);
        System.out.println(appRelDescriptor);
        if (appRelDescriptor.getDynamicAnalysisStatusType() != null && appRelDescriptor.getDynamicAnalysisStatusType().equals("In_Progress")) {
            FoDScanDescriptor scanDescriptor = FoDScanHelper.getScanDescriptor(unirest, String.valueOf(appRelDescriptor.getCurrentDynamicScanId()));
            if (skipIfRunning) {
                return scanDescriptor.asObjectNode().put("__action__", "SKIPPED_RUNNING");
            } else {
                throw new ValidationException("A dynamic scan with id '" + "" + String.valueOf(appRelDescriptor.getCurrentDynamicScanId()) +
                        "' is already in progress for release: " + appRelDescriptor.getReleaseName());
            }
        }

        // get current setup and check if its valid
        FoDDastScanSetupDescriptor currentSetup = FoDDastScanHelper.getSetupDescriptor(unirest, relId);
        if (currentSetup.getDynamicSiteURL() == null || StringUtils.isEmpty(currentSetup.getDynamicSiteURL())) {
            throw new ValidationException("The dynamic scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Dynamic Site URL' is missing or empty.");
        }

        if (remediation != null && remediation) {
            // if requesting a remediation scan make sure there are
            entitlementToUse = FoDDastScanHelper.validateRemediationEntitlement(unirest, relId, currentSetup.getEntitlementId());
        } else if (assessmentType.getAssessmentType() != null && entitlementType.getEntitlementType() != null) {
            // if assessment and entitlement type are both specified, find entitlement to use
            entitlementToUse = FoDDastScanHelper.getEntitlementToUse(unirest, relId,
                    assessmentType.getAssessmentType(), entitlementType.getEntitlementType());
        } else {
            // use the current scan setup
            entitlementToUse.copyFromCurrentSetup(currentSetup);
        }

        if (entitlementToUse.getEntitlementId() == null || entitlementToUse.getEntitlementId() <= 0)
            throw new ValidationException("Could not find a valid FoD entitlement to use.");

        String startDateStr = (startDate == null || startDate.isEmpty())
            ? LocalDateTime.now().format(dtf)
            : LocalDateTime.parse(startDate, dtf).toString();
        FoDStartDastScanRequest startScanRequest = new FoDStartDastScanRequest()
                .setStartDate(startDateStr)
                .setAssessmentTypeId(entitlementToUse.getAssessmentTypeId())
                .setEntitlementId(entitlementToUse.getEntitlementId())
                .setEntitlementFrequencyType(entitlementToUse.getFrequencyType())
                .setRemediationScan(remediation)
                .setApplyPreviousScanSettings(true)
                .setScanMethodType("Other")
                .setScanTool(fcliProperties.getProperty("projectName", "fcli"))
                .setScanToolVersion(fcliProperties.getProperty("projectVersion", "unknown"));

        //System.out.println(startScanRequest);
        return FoDDastScanHelper.startScan(unirest, Integer.valueOf(appRelResolver.getAppRelId(unirest)), startScanRequest).asJsonNode();
    }
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDDastScanHelper.renameFields(record);
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
