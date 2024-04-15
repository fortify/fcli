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
package com.fortify.cli.ssc.report.helper;

/**
 * Taken from SSC 23.2 REST documentation, commenting out types that
 * are not listed in the SSC UI.
 */
public enum SSCReportParameterType {
    SINGLE_PROJECT, 
    //SINGLE_RUNTIME_APP, 
    //SINGLE_SSA_PROJECT, 
    MULTI_PROJECT, 
    //MULTI_RUNTIME_APP, 
    //MULTI_SSA_PROJECT, 
    PROJECT_ATTRIBUTE, 
    STRING, 
    BOOLEAN, 
    DATE, 
    SINGLE_SELECT_DEFAULT, 
    //METADEF_GUID
}
