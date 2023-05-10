package com.fortify.cli.util.msp_report.generator.ssc;

import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Location;
import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Name;
import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_License_Type;

import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionEmbedderSupplier;
import com.fortify.cli.ssc.entity.attribute_definition.domain.SSCAttributeDefinitionType;
import com.fortify.cli.ssc.entity.attribute_definition.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;
import com.fortify.cli.ssc.rest.bulk.SSCBulkEmbedder;
import com.fortify.cli.ssc.rest.helper.SSCPagingHelper;
import com.fortify.cli.util.msp_report.collector.MspReportAppVersionCollector;
import com.fortify.cli.util.msp_report.collector.MspReportResultsCollector;
import com.fortify.cli.util.msp_report.config.MspReportSSCSourceConfig;
import com.fortify.cli.util.msp_report.generator.AbstractMspReportUnirestResultsGenerator;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppVersionDescriptor.MspReportSSCAppVersionProcessingStatus;

import io.micrometer.common.util.StringUtils;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;

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
        SSCPagingHelper.pagedRequest(unirest().get("/api/v1/projectVersions?limit=100"))
            .forEach(this::processAppVersionPage);
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
    
    /**
     * This method processes a single page of application versions,
     * embedding additional data using {@link #appVersionBulkEmbedder}
     * and then invoking {@link #processAppVersion(JsonNode)} for each
     * application version.
     */
    private void processAppVersionPage(HttpResponse<JsonNode> response) {
        appVersionBulkEmbedder.transformInput(unirest(), response.getBody())
            .forEach(this::processAppVersion);
    }
    
    /**
     * This method handles a single application version represented by 
     * the given {@link JsonNode}. The node will be converted to an
     * {@link MspReportSSCAppVersionDescriptor} instance, which is passed
     * to the {@link #processAppVersion(MspReportSSCAppVersionDescriptor)}
     * method for further processing. Based on processing state, a new
     * {@link MspReportSSCProcessedAppVersionDescriptor} instance will
     * then be created and passed to the {@link MspReportAppVersionCollector}
     * provided by the configured {@link MspReportResultsCollector}.
     */
    private void processAppVersion(JsonNode appVersionNode) {
        var descriptor = JsonHelper.treeToValue(appVersionNode, MspReportSSCAppVersionDescriptor.class);
        try {
            var summary = processAppVersion(descriptor.check());
            resultsCollector().appVersionCollector().report(sourceConfig(), new MspReportSSCProcessedAppVersionDescriptor(descriptor, MspReportSSCAppVersionProcessingStatus.success, "Successfully processed", summary));
        } catch ( Exception e ) {
            resultsCollector().errorWriter().addReportError("Error loading data for application version "+descriptor.getAppAndVersionName(), e);
            resultsCollector().appVersionCollector().report(sourceConfig(), new MspReportSSCProcessedAppVersionDescriptor(descriptor, MspReportSSCAppVersionProcessingStatus.error, e.getMessage(), createSummaryDescriptor(descriptor)));
        }
    }

    private MspReportSSCAppVersionEntitlementSummaryDescriptor processAppVersion(MspReportSSCAppVersionDescriptor descriptor) {
        var result = createSummaryDescriptor(descriptor);
        // TODO - Load artifacts within reporting period
        //      - Increase count for every artifact found
        //      - Write artifact details to CSV file
        int nrOfScans = new Random().nextInt(500);
        for ( int i=0 ; i<nrOfScans ; i++ ) {
            result.increaseNumberOfScansInReportingPeriod();
        }
        return result;
    }

    private MspReportSSCAppVersionEntitlementSummaryDescriptor createSummaryDescriptor(MspReportSSCAppVersionDescriptor descriptor) {
        return new MspReportSSCAppVersionEntitlementSummaryDescriptor(descriptor.getMspLicenseType());
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
            unirest.config().addDefaultHeader("Authorization", "FortifyToken "+SSCTokenConverter.toRestToken(token));
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
