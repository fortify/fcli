package com.fortify.cli.util.msp_report.generator.ssc;

import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Location;
import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Name;
import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_License_Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class MspReportSSCAppVersionDescriptor extends JsonNodeHolder {
    @JsonProperty("id") private String versionId;
    private String applicationName;
    @JsonProperty("name") private String versionName;
    private boolean active;
    private MspReportLicenseType mspLicenseType;
    private String mspEndCustomerName;
    private String mspEndCustomerLocation;
    
    public void setProject(ObjectNode project) {
        applicationName = project.get("name").asText();
    }
    
    public void setAttrValuesByName(ObjectNode attrValuesByName) {
        mspLicenseType = JsonHelper.evaluateSpelExpression(attrValuesByName, MSP_License_Type.asExpression(), MspReportLicenseType.class);
        mspEndCustomerName = JsonHelper.evaluateSpelExpression(attrValuesByName, MSP_End_Customer_Name.asExpression(), String.class);
        mspEndCustomerLocation = JsonHelper.evaluateSpelExpression(attrValuesByName, MSP_End_Customer_Location.asExpression(), String.class);
    }

    public MspReportSSCAppVersionDescriptor check() {
        // Even though we have already validated the attribute definitions,
        // attribute values may still be blank if an application version
        // was created before the attributes were created/set to required.
        checkNotBlankAttr("mspLicenseType", mspLicenseType);
        return this;
    }
    
    @JsonIgnore
    public String getAppAndVersionName() {
        return applicationName+":"+versionName;
    }

    private void checkNotBlankAttr(String name, Object value) {
        if ( value==null || (value instanceof String && ((String) value).isBlank()) ) {
            throw new IllegalStateException(String.format("Blank value not allowed for attribute %s (%s)", name, getAppAndVersionName()));
        }
    }

    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode.put("applicationName", applicationName)
                .put("versionName", versionName)
                .put("versionId", versionId)
                .put("active", active)
                .put("mspLicenseType", mspLicenseType.name())
                .put("mspEndCustomerName", mspEndCustomerName)
                .put("mspEndCustomerLocation", mspEndCustomerLocation)
                ;
    }
}
