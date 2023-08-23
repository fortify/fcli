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

package com.fortify.cli.fod.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod.scan.helper.sast.FoDScanSastStartRequest;
import com.fortify.cli.fod.scan_config.helper.FoDScanConfigSastDescriptor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Properties;

@Command(name = FoDOutputHelperMixins.StartSast.CMD_NAME)
public class FoDScanStartSastCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.StartSast outputHelper;

    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;

    @Option(names = {"--remediation"})
    private Boolean remediationScan = false;
    @Option(names = {"--notes"})
    private String notes;
    @Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            String relId = releaseDescriptor.getReleaseId();

            validateScanSetup(unirest, relId);

            FoDScanSastStartRequest startScanRequest = FoDScanSastStartRequest.builder()
                    .isRemediationScan(remediationScan)
                    .scanMethodType("Other")
                    .notes(notes != null && !notes.isEmpty() ? notes : "")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown"))
                    .build();

            return FoDScanSastHelper.startScanWithDefaults(unirest, releaseDescriptor, startScanRequest, scanFile).asJsonNode();
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

    private void validateScanSetup(UnirestInstance unirest, String relId) {
        // get current setup and check if its valid
        FoDScanConfigSastDescriptor currentSetup = FoDScanSastHelper.getSetupDescriptor(unirest, relId);
        if (currentSetup.getEntitlementId() == null || currentSetup.getEntitlementId() <= 0) {
            throw new IllegalStateException("The static scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Entitlement' is missing or empty.");
        }
        if (StringUtils.isBlank(currentSetup.getTechnologyStack())) {
            throw new IllegalStateException("The static scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Technology Stack/Language Level' is missing or empty.");
        }
    }

}
