package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Reflectable
public class AuditResponse {
    private AuditResult auditResult;
    private int inputToken;
    private int outputToken;


    private String status;
    private String statusMessage;
    private String issueId;

    private String tier;
    private String aviatorPredictionTag;
    private Boolean isAviatorProcessed;
    private String userPrompt;
    private String systemPrompt;

}
