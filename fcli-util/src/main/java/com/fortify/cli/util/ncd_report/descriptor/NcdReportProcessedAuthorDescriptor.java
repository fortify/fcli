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
