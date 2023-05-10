package com.fortify.cli.util.msp_report.generator.ssc;

public enum MspReportSSCAppVersionAttribute {
    MSP_License_Type, MSP_End_Customer_Name, MSP_End_Customer_Location;
    
    public String asExpression() {
        return String.format("#this['%s']", name());
    }
}
