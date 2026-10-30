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

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Scan-level condition that may explain issue status transitions for any issue
 *
 * @param cause Cause represented by this condition
 * @param direction Direction of the change
 * @param evidence Human-readable evidence, like changed setting values or statistics
 */
public record FoDScanCondition(FoDAttributionCause cause, FoDAttributionDirection direction, List<String> evidence) {
    public FoDAttributionEffects getEffects() {
        return cause.getEffects(direction);
    }

    public ObjectNode toJson() {
        var result = JsonHelper.getObjectMapper().createObjectNode()
                .put("cause", cause.name())
                .put("direction", direction.name());
        result.set("explains", getEffects().toJson());
        var evidenceArray = result.putArray("evidence");
        evidence.forEach(evidenceArray::add);
        return result;
    }
}
