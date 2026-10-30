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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Single attribution of an issue status transition to a cause
 *
 * @param cause Attributed cause
 * @param basis Basis of the attribution
 * @param scope Whether this attribution is based on issue-specific evidence or a scan-level condition
 * @param evidence Human-readable evidence for issue-level attributions; scan-level evidence is listed with the scan condition
 */
public record FoDAttribution(FoDAttributionCause cause, FoDAttributionBasis basis, Scope scope, String evidence) {
    public enum Scope { ISSUE, SCAN }

    public ObjectNode toJson() {
        var result = JsonHelper.getObjectMapper().createObjectNode()
                .put("cause", cause.name())
                .put("basis", basis.name())
                .put("scope", scope.name());
        if ( evidence != null ) { result.put("evidence", evidence); }
        return result;
    }
}
