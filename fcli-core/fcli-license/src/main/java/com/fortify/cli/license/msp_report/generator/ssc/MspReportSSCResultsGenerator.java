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

import static com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Location;
import static com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Name;
import static com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_License_Type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.license.msp_report.collector.MspReportAppScanCollector;
import com.fortify.cli.license.msp_report.collector.MspReportAppScanCollector.MspReportScanCollectorState;
import com.fortify.cli.license.msp_report.collector.MspReportResultsCollector;
import com.fortify.cli.license.msp_report.config.MspReportSSCSourceConfig;
import com.fortify.cli.license.msp_report.generator.AbstractMspReportUnirestResultsGenerator;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkEmbedder;
import com.fortify.cli.ssc._common.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc._common.rest.helper.SSCPagingHelper;
import com.fortify.cli.ssc._common.rest.helper.SSCPagingHelper.SSCContinueNextPageSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionEmbedderSupplier;
import com.fortify.cli.ssc.attribute_definition.domain.SSCAttributeDefinitionType;
import com.fortify.cli.ssc.attribute_definition.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.token.helper.SSCTokenConverter;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.UnirestInstance;

/**
 * This class is responsible for loading MSP data from SSC.
 * 
 * @author rsenden
 *
 */
public class MspReportSSCResultsGenerator extends AbstractMspReportUnirestResultsGenerator<MspReportSSCSourceConfig> {
    private final SSCBulkEmbedder appVersionBulkEmbedder = new SSCBulkEmbedder(SSCAppVersionEmbedderSupplier.attrValuesByName);

    /**
     * Constructor to configure this instance with the given 
     * {@link MspReportSSCSourceConfig} and
     * {@link MspReportResultsCollector}.
     */
    public MspReportSSCResultsGenerator(MspReportSSCSourceConfig sourceConfig, MspReportResultsCollector resultsCollector) {        
        super(sourceConfig, resultsCollector);
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
        SSCPagingHelper.pagedRequest(unirest().get("/api/v1/projects?limit=100"))
            .forEach(this::processAppPage);
    }
    
    /**
     * Validate SSC application version attributes
     */
    private void validateSSCAttributes() {
        var attrDefHelper = new SSCAttributeDefinitionHelper(unirest());
        attrDefHelper.getAttributeDefinitionDescriptor(MSP_License_Type.name())
            .check(true, SSCAttributeDefinitionType.SINGLE, "Application", "Scan", "Demo");
        attrDefHelper.getAttributeDefinitionDescriptor(MSP_End_Customer_Name.name())
            .check(false, SSCAttributeDefinitionType.TEXT);
        attrDefHelper.getAttributeDefinitionDescriptor(MSP_End_Customer_Location.name())
            .check(false, SSCAttributeDefinitionType.TEXT);
    }
    
    private void processAppPage(HttpResponse<JsonNode> response) {
        var apps = ((ArrayNode)SSCInputTransformer.getDataOrSelf(response.getBody()));
        JsonHelper.stream(apps).forEach(this::processApp);
    }
    
    private void processApp(JsonNode appNode) {
        var descriptor = JsonHelper.treeToValue(appNode, MspReportSSCAppDescriptor.class);
        resultsCollector().progressWriter().writeI18nProgress("processing.app", descriptor.getName());
        try {
            loadVersionsForApp(descriptor);
            descriptor.check(resultsCollector().logger());
            var summary = processApp(descriptor);
            var status = descriptor.getWarnCounter().getCount()>0 
                    ? MspReportProcessingStatus.warn
                    : MspReportProcessingStatus.success;
            var reason = status==MspReportProcessingStatus.warn
                    ? "Processed with warnings"
                    : "Successfully processed";
            resultsCollector().appCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppDescriptor(descriptor, status, reason, summary));
        } catch ( Exception e ) {
            resultsCollector().logger().error("Error Processing application %s", e, descriptor.getName());
            resultsCollector().appCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppDescriptor(descriptor, MspReportProcessingStatus.error, e.getMessage(), new MspReportSSCAppSummaryDescriptor()));
        }
    }
    
    private void loadVersionsForApp(MspReportSSCAppDescriptor descriptor) {
        SSCPagingHelper.pagedRequest(
            unirest().get("/api/v1/projects/{id}/versions?limit=100")
                .routeParam("id", descriptor.getId()))
            .forEach(r->loadAppVersionPage(descriptor, r.getBody()));
    }
    
    private void loadAppVersionPage(MspReportSSCAppDescriptor appDescriptor, JsonNode body) {
        var appVersions = appVersionBulkEmbedder.transformInput(unirest(), body);
        JsonHelper.stream(appVersions)
            .map(node->JsonHelper.treeToValue(node, MspReportSSCAppVersionDescriptor.class))
            .forEach(versionDescriptor -> appDescriptor.addVersionDescriptor(resultsCollector().logger(), versionDescriptor));
    }

    private MspReportSSCAppSummaryDescriptor processApp(MspReportSSCAppDescriptor appDescriptor) {
        try ( var scanCollector = resultsCollector().scanCollector(sourceConfig(), appDescriptor) ) {
            appDescriptor.getVersionDescriptors()
                .forEach(versionDescriptor->processAppVersion(versionDescriptor, scanCollector));
            return scanCollector.summary();
        }
    }
    
    private void processAppVersion(MspReportSSCAppVersionDescriptor versionDescriptor, MspReportAppScanCollector scanCollector) {
        try {
            var continueNextPageSupplier = new SSCContinueNextPageSupplier();
            HttpRequest<?> req = unirest().get("/api/v1/projectVersions/{pvId}/artifacts?limit=100&embed=scans")
                    .routeParam("pvId", versionDescriptor.getVersionId());
            SSCPagingHelper.pagedRequest(req, continueNextPageSupplier)
                .forEach(r->processArtifactPage(r.getBody(), versionDescriptor, scanCollector, continueNextPageSupplier));
            resultsCollector().appVersionCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppVersionDescriptor(versionDescriptor, MspReportProcessingStatus.success, "Successfully processed"));
        } catch ( Exception e ) {
            resultsCollector().logger().error("Error loading artifacts for application version %s", e, versionDescriptor.getAppAndVersionName());
            resultsCollector().appVersionCollector()
                .report(sourceConfig(), new MspReportSSCProcessedAppVersionDescriptor(versionDescriptor, MspReportProcessingStatus.error, e.getMessage()));
            throw e;
        }
    }

    private void processArtifactPage(JsonNode body, MspReportSSCAppVersionDescriptor versionDescriptor, MspReportAppScanCollector scanCollector, SSCContinueNextPageSupplier continueNextPageSupplier) {
        var done = JsonHelper.stream((ArrayNode)SSCInputTransformer.getDataOrSelf(body))
            .map(this::createArtifactDescriptor)
            .peek(d->resultsCollector().artifactCollector().report(sourceConfig(), versionDescriptor, d))
            .flatMap(MspReportSSCScanDescriptor::from)
            .map(scanCollector::report)
            .filter(MspReportScanCollectorState.DONE::equals)
            .findFirst()
            .isPresent();
        continueNextPageSupplier.setLoadNextPage(!done);
    }
    
    private MspReportSSCArtifactDescriptor createArtifactDescriptor(JsonNode artifactNode) {
        return JsonHelper.treeToValue(artifactNode, MspReportSSCArtifactDescriptor.class);
    }

    /**
     * Add the Authorization header to the configuration
     * of the given {@link UnirestInstance}, based on the
     * tokenExpression provided in the source configuration. 
     */
    @Override
    protected void configure(UnirestInstance unirest) {
        String tokenExpression = sourceConfig().getTokenExpression();
        if ( StringUtils.isBlank(tokenExpression) ) {
            throw new IllegalArgumentException("SSC configuration requires tokenExpression property");
        }
        // TODO Doesn't really make sense to use this method with null input object
        //      We should have a corresponding method in SpelHelper that doesn't take
        //      any input
        String token = JsonHelper.evaluateSpelExpression(null, tokenExpression, String.class);
        if ( StringUtils.isBlank(token) ) {
            throw new IllegalStateException("No token found from expression: "+tokenExpression);
        } else {
            unirest.config().setDefaultHeader("Authorization", "FortifyToken "+SSCTokenConverter.toRestToken(token));
        }
    }
    
    /**
     * Return the source type, 'ssc' in this case.
     */
    @Override
    protected String getType() {
        return "ssc";
    }
}
