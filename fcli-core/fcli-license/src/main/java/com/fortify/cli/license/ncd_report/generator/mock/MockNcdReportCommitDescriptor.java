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
package com.fortify.cli.license.ncd_report.generator.mock;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportCommitDescriptor;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple mock implementation of {@link INcdReportCommitDescriptor}.
 */
@Data @AllArgsConstructor
public class MockNcdReportCommitDescriptor implements INcdReportCommitDescriptor {
    private String sha;
    private OffsetDateTime dateTime;
    
    @Override
    public String getId() {
        return sha;
    }
    
    @Override
    public LocalDateTime getDate() {
        return dateTime.toLocalDateTime();
    }
    
    @Override
    public String getMessage() {
        return "Mock commit " + sha;
    }
    
    public ObjectNode asJsonNode() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("sha", sha);
        node.put("commit", JsonNodeFactory.instance.objectNode()
            .put("author", JsonNodeFactory.instance.objectNode()
                .put("date", dateTime.toString())));
        return node;
    }
}
