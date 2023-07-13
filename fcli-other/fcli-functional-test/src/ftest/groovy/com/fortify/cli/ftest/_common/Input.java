/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.ftest._common;

public enum Input {
    TestsToRun("run"), 
    FcliCommand("fcli"),
    JavaCommand("java");
    
    private Input(String suffix) {
        this.propertySuffix = suffix;
    }
    
    private final String propertySuffix;
    
    public String propertyName() {
        return addPropertyPrefix(propertySuffix);
    }
    
    public String get() {
        return System.getProperty(propertyName());
    }
    
    public static String addPropertyPrefix(String propertySuffix) {
        return "ft." + propertySuffix;
    }
}
