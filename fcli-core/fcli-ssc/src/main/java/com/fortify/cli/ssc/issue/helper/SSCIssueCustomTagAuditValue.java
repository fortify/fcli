/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ssc.issue.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a custom tag value in an SSC issue audit request.
 */
@JsonInclude(Include.NON_NULL)
@Reflectable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSCIssueCustomTagAuditValue {
    @JsonProperty("customTagGuid")
    private String customTagGuid;
    
    @JsonProperty("textValue")
    private String textValue;
    
    @JsonProperty("newCustomTagIndex")
    private Integer newCustomTagIndex;
    
    @JsonProperty("dateValue")
    private String dateValue;
    
    @JsonProperty("decimalValue")
    private Double decimalValue;
    
    public static SSCIssueCustomTagAuditValue forText(String guid, String value) {
        SSCIssueCustomTagAuditValue result = new SSCIssueCustomTagAuditValue();
        result.setCustomTagGuid(guid);
        result.setTextValue(value);
        return result;
    }
    
    public static SSCIssueCustomTagAuditValue forList(String guid, Integer lookupIndex) {
        SSCIssueCustomTagAuditValue result = new SSCIssueCustomTagAuditValue();
        result.setCustomTagGuid(guid);
        result.setNewCustomTagIndex(lookupIndex);
        return result;
    }
    
    public static SSCIssueCustomTagAuditValue forDate(String guid, String dateValue) {
        SSCIssueCustomTagAuditValue result = new SSCIssueCustomTagAuditValue();
        result.setCustomTagGuid(guid);
        result.setDateValue(dateValue);
        return result;
    }
    
    public static SSCIssueCustomTagAuditValue forDecimal(String guid, Double value) {
        SSCIssueCustomTagAuditValue result = new SSCIssueCustomTagAuditValue();
        result.setCustomTagGuid(guid);
        result.setDecimalValue(value);
        return result;
    }
}
