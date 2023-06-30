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
package com.fortify.cli.util.msp_report.generator.ssc;

import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Location;
import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_End_Customer_Name;
import static com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionAttribute.MSP_License_Type;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class MspReportSSCAppVersionDescriptor extends JsonNodeHolder {
    // We need to exclude this field from toString, equals and hashCode to
    // avoid endless recursion
    @ToString.Exclude @EqualsAndHashCode.Exclude private MspReportSSCAppDescriptor appDescriptor;
    @JsonProperty("id") private String versionId;
    @JsonProperty("name") private String versionName;
    @JsonProperty("creationDate") private ZonedDateTime versionCreationDate;
    private boolean active;
    // MSP attribute values should only be accessed by MspReportSSCAppDescriptor,
    // hence access level PACKAGE
    @Getter(AccessLevel.PACKAGE) private MspReportLicenseType mspLicenseType;
    @Getter(AccessLevel.PACKAGE) private String mspEndCustomerName;
    @Getter(AccessLevel.PACKAGE) private String mspEndCustomerLocation;
    
    public void setAttrValuesByName(ObjectNode attrValuesByName) {
        mspLicenseType = JsonHelper.evaluateSpelExpression(attrValuesByName, MSP_License_Type.asExpression(), MspReportLicenseType.class);
        mspEndCustomerName = JsonHelper.evaluateSpelExpression(attrValuesByName, MSP_End_Customer_Name.asExpression(), String.class);
        mspEndCustomerLocation = JsonHelper.evaluateSpelExpression(attrValuesByName, MSP_End_Customer_Location.asExpression(), String.class);
    }

    public void setAppDescriptor(MspReportSSCAppDescriptor appDescriptor) {
        this.appDescriptor = appDescriptor;
    }
    
    @JsonIgnore
    public String getAppAndVersionName() {
        return appDescriptor.getName()+":"+versionName;
    }

    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return appDescriptor.updateReportRecord(objectNode)
                .put("versionId", versionId)
                .put("versionName", versionName)
                .put("versionCreationDate", versionCreationDate.toString())
                .put("active", active)
                .put("mspLicenseType", mspLicenseType.name())
                .put("mspEndCustomerName", mspEndCustomerName)
                .put("mspEndCustomerLocation", mspEndCustomerLocation)
                ;
    }
}
