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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.report.logger.IReportLogger;
import com.fortify.cli.common.util.Counter;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor 
@Data @EqualsAndHashCode(callSuper = false)
public class MspReportSSCAppDescriptor extends JsonNodeHolder {
    private String id;
    private String name;
    private ZonedDateTime creationDate;
    private MspReportLicenseType mspLicenseType;
    private String mspEndCustomerName;
    private String mspEndCustomerLocation;
    private final List<MspReportSSCAppVersionDescriptor> versionDescriptors = new ArrayList<>(); 
    private final Counter warnCounter = new Counter();

    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode
                .put("applicationId", id)
                .put("applicationName", name)
                .put("applicationCreationDate", creationDate.toString())
                .put("mspLicenseType", mspLicenseType.name())
                .put("mspEndCustomerName", mspEndCustomerName)
                .put("mspEndCustomerLocation", mspEndCustomerLocation);
    }
    
    public void check(IReportLogger logger) {
        if ( mspLicenseType==null ) {
            throw new IllegalStateException("Missing license type");
        }
        
        if ( mspLicenseType!=MspReportLicenseType.Demo ) {
            if ( StringUtils.isBlank(mspEndCustomerName) ) {
                warn(logger, "Missing MSP end customer name");
            }
            if ( StringUtils.isBlank(mspEndCustomerLocation) ) {
                warn(logger, "Missing MSP end customer location");
            }
        }
    }

    public void addVersionDescriptor(IReportLogger logger, MspReportSSCAppVersionDescriptor appVersionDescriptor) {
        appVersionDescriptor.setAppDescriptor(this);
        versionDescriptors.add(appVersionDescriptor);
        mspLicenseType = get(logger, MSP_License_Type.name(), mspLicenseType, appVersionDescriptor.getMspLicenseType(), this::onMismatch);
        mspEndCustomerName = get(logger, MSP_End_Customer_Name.name(), mspEndCustomerName, appVersionDescriptor.getMspEndCustomerName(), null);
        mspEndCustomerLocation = get(logger, MSP_End_Customer_Location.name(), mspEndCustomerLocation, appVersionDescriptor.getMspEndCustomerLocation(), null);
    }
    
    private MspReportLicenseType onMismatch(MspReportLicenseType appType, MspReportLicenseType versionType) {
        var allByPriority = new ArrayList<>(MspReportLicenseType.allOrderedByPriority());
        var currentTypes = Arrays.asList(appType, versionType);
        allByPriority.retainAll(currentTypes);
        return allByPriority.get(0);
    }
    
    private <T> T get(IReportLogger logger, String attrName, T appValue, T versionValue, BiFunction<T,T,T> onMismatch) {
        if ( appValue==null || (appValue instanceof String && ((String) appValue).isBlank())) {
            return versionValue;
        }
        if ( versionValue==null || (versionValue instanceof String && ((String) versionValue).isBlank())) {
            return appValue;
        }
        if ( appValue.equals(versionValue) ) { return appValue; }
        var msg = String.format("%s mismatch (app: %s, version: %s)", attrName, appValue, versionValue);
        if ( onMismatch!=null ) {
            var value = onMismatch.apply(appValue, versionValue);
            warn(logger, "%s, using %s", msg, value);
            return value;
        } else {
            throw new IllegalStateException(msg);
        }
    }
    
    private void warn(IReportLogger logger, String msg, Object... msgArgs) {
        var fullMsg = String.format("Application %s (%s): %s", name, mspLicenseType, msg);
        logger.warn(fullMsg, msgArgs);
        warnCounter.increase();
    }
    
}
