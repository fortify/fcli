package com.fortify.cli.util.msp_report.generator.ssc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionEmbedderSupplier;
import com.fortify.cli.ssc.entity.attribute_definition.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;
import com.fortify.cli.ssc.rest.bulk.SSCBulkEmbedder;
import com.fortify.cli.ssc.rest.helper.SSCPagingHelper;
import com.fortify.cli.util.msp_report.collector.MspReportResultsCollector;
import com.fortify.cli.util.msp_report.config.MspReportSSCSourceConfig;
import com.fortify.cli.util.msp_report.generator.AbstractMspReportUnirestResultsGenerator;

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
    private static final String ATTR_LICENSE_TYPE = "MSP_License_Type";
    private static final String ATTR_END_CUSTOMER_NAME = "MSP_End_Customer_Name";
    private static final String ATTR_END_CUSTOMER_LOCATION = "MSP_End_Customer_Location";
    private final SSCBulkEmbedder appVersionBulkEmbedder = new SSCBulkEmbedder(SSCAppVersionEmbedderSupplier.attrsByName);

    /**
     * Constructor to configure this instance with the given 
     * {@link MspReportSSCSourceConfig} and
     * {@link MspReportResultsCollector}.
     */
    public MspReportSSCResultsGenerator(MspReportSSCSourceConfig sourceConfig, MspReportResultsCollector resultsCollector) {        
        super(sourceConfig, resultsCollector);
    }

    /**
     * Primary method for running the generation process, taking the
     * {@link UnirestInstance} provided by our superclass.
     */
    @Override
    protected void generateResults() {
        validateRequiredAttributesPresent();
        SSCPagingHelper.pagedRequest(unirest().get("/api/v1/projectVersions?limit=100"))
            .forEach(this::handleAppVersionPage);
    }
    
    private void validateRequiredAttributesPresent() {
        var attrDefHelper = new SSCAttributeDefinitionHelper(unirest());
        checkRequiredAttrPresent(attrDefHelper, ATTR_LICENSE_TYPE);
        checkRequiredAttrPresent(attrDefHelper, ATTR_END_CUSTOMER_NAME);
        checkRequiredAttrPresent(attrDefHelper, ATTR_END_CUSTOMER_LOCATION);
    }
    
    private void checkRequiredAttrPresent(SSCAttributeDefinitionHelper attrDefHelper, String name) {
        if ( !attrDefHelper.getAttributeDefinitionDescriptor(name).isRequired() ) {
            throw new IllegalStateException("SSC attribute "+name+" must be configured as required attribute");
        }
    }

    private void handleAppVersionPage(HttpResponse<JsonNode> response) {
        appVersionBulkEmbedder.transformInput(unirest(), response.getBody())
            .forEach(this::handleAppVersion);
    }
    
    private void handleAppVersion(JsonNode appVersionNode) {
        System.out.println(appVersionNode.toPrettyString());
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
