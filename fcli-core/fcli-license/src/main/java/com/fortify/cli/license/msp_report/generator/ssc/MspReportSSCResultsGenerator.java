/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.msp_report.generator.ssc;

import static com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Location;
import static com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Name;
import static com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_License_Type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.license.msp_report.collector.MspReportAppScanCollector;
import com.fortify.cli.license.msp_report.collector.MspReportContext;
import com.fortify.cli.license.msp_report.config.MspReportSSCSourceConfig;
import com.fortify.cli.license.msp_report.generator.AbstractMspReportResultsGenerator;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkEmbedder;
import com.fortify.cli.ssc._common.rest.ssc.helper.SSCPagingHelper.SSCContinueNextPageSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionEmbedderSupplier;
import com.fortify.cli.ssc.attribute.domain.SSCAttributeDefinitionType;
import com.fortify.cli.ssc.attribute.helper.SSCAttributeDefinitionHelper;

import kong.unirest.HttpResponse;

/**
 * This class is responsible for loading MSP data from SSC.
 * 
 * @author rsenden
 *
 */
public class MspReportSSCResultsGenerator extends AbstractMspReportResultsGenerator<MspReportSSCSourceConfig> {
    private final SSCBulkEmbedder appVersionBulkEmbedder = new SSCBulkEmbedder(SSCAppVersionEmbedderSupplier.attrValuesByName);
    
    /**
     * REST helper for SSC API operations. A new instance is created for each
     * source configuration, ensuring proper isolation of API credentials and
     * configuration (base URL, token, etc.).
     */
    private final MspReportSSCRestHelper restHelper;
    private final MspReportSSCUnirestInstanceSupplier unirestInstanceSupplier;

    /**
     * Constructor to configure this instance with the given 
     * {@link MspReportSSCSourceConfig} and
     * {@link MspReportContext}.
     */
    public MspReportSSCResultsGenerator(MspReportSSCSourceConfig sourceConfig, MspReportContext reportContext) {        
        super(sourceConfig, reportContext);
        this.unirestInstanceSupplier = new MspReportSSCUnirestInstanceSupplier(reportContext.unirestContext(), sourceConfig);
        this.restHelper = new MspReportSSCRestHelper(unirestInstanceSupplier);
    }

    /**
     * Primary method for running the generation process. This method validates
     * required application version attributes are correctly configured, then
     * loads all SSC application versions pages, invoking 
     * {@link #processAppVersionPage(HttpResponse)} for each page.
     */
    @Override
    protected void generateResults() {
        validateSSCAttributes();
        restHelper.processProjectPages(this::processAppPage);
    }
    
    /**
     * Validate SSC application version attributes
     */
    private void validateSSCAttributes() {
        var attrDefHelper = new SSCAttributeDefinitionHelper(restHelper.getUnirest());
        attrDefHelper.getAttributeDefinitionDescriptor(MSP_License_Type.name())
            .check(true, SSCAttributeDefinitionType.SINGLE, "Application", "Scan", "Demo");
        attrDefHelper.getAttributeDefinitionDescriptor(MSP_End_Customer_Name.name())
            .check(false, SSCAttributeDefinitionType.TEXT);
        attrDefHelper.getAttributeDefinitionDescriptor(MSP_End_Customer_Location.name())
            .check(false, SSCAttributeDefinitionType.TEXT);
    }
    
    private void processAppPage(HttpResponse<JsonNode> response) {
        var apps = restHelper.extractData(response.getBody());
        JsonHelper.stream(apps).forEach(this::processApp);
    }
    
    private void processApp(JsonNode appNode) {
        var descriptor = JsonHelper.treeToValue(appNode, MspReportSSCAppDescriptor.class);
        reportContext().progressWriter().writeI18nProgress("processing.app", descriptor.getName());
        try {
            loadVersionsForApp(descriptor);
            descriptor.check(reportContext().logger());
            var summary = processApp(descriptor);
            var status = descriptor.getWarnCounter().getCount()>0 
                    ? MspReportProcessingStatus.warn
                    : MspReportProcessingStatus.success;
            var reason = status==MspReportProcessingStatus.warn
                    ? "Processed with warnings"
                    : "Successfully processed";
            reportContext().appCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppDescriptor(descriptor, status, reason, summary));
        } catch ( Exception e ) {
            reportContext().logger().error("Error Processing application %s", e, descriptor.getName());
            reportContext().appCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppDescriptor(descriptor, MspReportProcessingStatus.error, e.getMessage(), new MspReportSSCAppSummaryDescriptor()));
        }
    }
    
    private void loadVersionsForApp(MspReportSSCAppDescriptor descriptor) {
        restHelper.processProjectVersionPages(descriptor.getId(), 
            r->loadAppVersionPage(descriptor, r.getBody()));
    }
    
    private void loadAppVersionPage(MspReportSSCAppDescriptor appDescriptor, JsonNode body) {
        var appVersions = appVersionBulkEmbedder.transformInput(restHelper.getUnirest(), body);
        JsonHelper.stream(appVersions)
            .map(node->JsonHelper.treeToValue(node, MspReportSSCAppVersionDescriptor.class))
            .forEach(versionDescriptor -> appDescriptor.addVersionDescriptor(reportContext().logger(), versionDescriptor));
    }

    private MspReportSSCAppSummaryDescriptor processApp(MspReportSSCAppDescriptor appDescriptor) {
        try ( var scanCollector = reportContext().scanCollector(sourceConfig(), appDescriptor) ) {
            appDescriptor.getVersionDescriptors()
                .forEach(versionDescriptor->processAppVersion(versionDescriptor, scanCollector));
            return scanCollector.summary();
        }
    }
    
    private void processAppVersion(MspReportSSCAppVersionDescriptor versionDescriptor, MspReportAppScanCollector scanCollector) {
        try {
            var continueNextPageSupplier = new SSCContinueNextPageSupplier();
            restHelper.processArtifactPages(versionDescriptor.getVersionId(), continueNextPageSupplier,
                r->processArtifactPage(r.getBody(), versionDescriptor, scanCollector, continueNextPageSupplier));
            reportContext().appVersionCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppVersionDescriptor(versionDescriptor, MspReportProcessingStatus.success, "Successfully processed"));
        } catch ( Exception e ) {
            reportContext().logger().error("Error loading artifacts for application version %s", e, versionDescriptor.getAppAndVersionName());
            reportContext().appVersionCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppVersionDescriptor(versionDescriptor, MspReportProcessingStatus.error, e.getMessage()));
            throw e;
        }
    }

    private void processArtifactPage(JsonNode body, MspReportSSCAppVersionDescriptor versionDescriptor, MspReportAppScanCollector scanCollector, SSCContinueNextPageSupplier continueNextPageSupplier) {
        var done = JsonHelper.stream(restHelper.extractData(body))
            .map(this::createArtifactDescriptor)
            .peek(d->reportContext().artifactCollector().report(sourceConfig(), versionDescriptor, d))
            .flatMap(MspReportSSCScanDescriptor::from)
            .map(scanCollector::report)
            .filter(Break.TRUE::equals)
            .findFirst()
            .isPresent();
        continueNextPageSupplier.setLoadNextPage(!done);
    }
    
    private MspReportSSCArtifactDescriptor createArtifactDescriptor(JsonNode artifactNode) {
        return JsonHelper.treeToValue(artifactNode, MspReportSSCArtifactDescriptor.class);
    }
    
    @Override
    public void close() {
        unirestInstanceSupplier.close();
    }
    
    /**
     * Return the source type, 'ssc' in this case.
     */
    @Override
    protected String getType() {
        return "ssc";
    }
}
