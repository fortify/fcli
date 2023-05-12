package com.fortify.cli.util.msp_report.generator.ssc;

import java.util.Arrays;
import java.util.List;

public enum MspReportLicenseType {
    Application, Scan, Demo;
    
    public static final List<MspReportLicenseType> allOrderedByPriority() {
        return Arrays.asList(Application, Scan, Demo);
    }
}
