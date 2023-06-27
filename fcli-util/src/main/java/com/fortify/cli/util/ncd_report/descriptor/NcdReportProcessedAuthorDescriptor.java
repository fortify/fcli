/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.ncd_report.descriptor;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class NcdReportProcessedAuthorDescriptor {
    private final INcdReportAuthorDescriptor authorDescriptor;
    private final NcdReportProcessedAuthorState state;
    private final int authorNumber;
    private final ObjectNode expressionInput;
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode.put("authorName", authorDescriptor.getName())
                .put("authorEmail", authorDescriptor.getEmail())
                .put("authorState", state.name())
                .put("authorNumber", authorNumber);
    }
    
    public static enum NcdReportProcessedAuthorState {
        processed, ignored
    }
}
