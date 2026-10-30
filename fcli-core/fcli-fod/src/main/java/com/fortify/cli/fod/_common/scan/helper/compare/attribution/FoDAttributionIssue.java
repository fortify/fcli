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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Issue (as returned by the FoD vulnerabilities endpoint) to be attributed, together with its status transition */
public record FoDAttributionIssue(ObjectNode node, FoDAttributionTransition transition) {
    /** @return Full issue location if available, otherwise the (potentially abbreviated) primary location */
    public String getLocation() {
        return StringUtils.firstNonBlank(getText("primaryLocationFull"), getText("primaryLocation"));
    }

    public String getCategory() {
        return getText("category");
    }

    /** @return Whether the issue has a dataflow source; FoD returns '&lt;None&gt;' for issues without dataflow */
    public boolean hasDataflowSource() {
        var source = StringUtils.trimToEmpty(getText("source"));
        return !source.isEmpty() && !"<None>".equalsIgnoreCase(source);
    }

    public String getText(String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
