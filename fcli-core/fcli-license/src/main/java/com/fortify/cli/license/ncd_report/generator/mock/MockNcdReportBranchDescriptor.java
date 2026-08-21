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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportBranchDescriptor;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple mock implementation of {@link INcdReportBranchDescriptor}.
 */
@Data @AllArgsConstructor
public class MockNcdReportBranchDescriptor implements INcdReportBranchDescriptor {
    private String name;
    private String sha;
    
    public ObjectNode asJsonNode() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("name", name);
        node.put("commit", JsonNodeFactory.instance.objectNode().put("sha", sha));
        return node;
    }
}
