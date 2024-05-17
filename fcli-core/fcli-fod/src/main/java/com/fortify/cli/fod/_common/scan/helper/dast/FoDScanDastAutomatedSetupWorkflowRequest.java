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

package com.fortify.cli.fod._common.scan.helper.dast;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FoDScanDastAutomatedSetupWorkflowRequest extends FoDScanDastAutomatedSetupBaseRequest {

    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Getter @ToString
    public static class WorkflowDrivenMacro {
        Integer fileId;
        ArrayList<String> allowedHosts;
    }

    @Builder.Default
    public String policy = "Standard"; // ['Standard', 'Api', 'CriticalsAndHighs', 'PassiveScan']
    public ArrayList<WorkflowDrivenMacro> workflowDrivenMacro;
}
