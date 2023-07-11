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
package com.fortify.cli.ssc.entity.report_template.domain;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class SSCReportParameterOption {
    public boolean defaultValue;
    public String description;
    public String displayValue;
    public int index;
    public String reportValue;
}
