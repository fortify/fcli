/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.fod.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

public class AviatorFoDApplyRemediationsHelper {
    public AviatorFoDApplyRemediationsHelper(){}


    /**
     * Builds the final JSON result node for the command output.
     * @param rd The SSCAppVersionDescriptor.
     * @param totalRemediation Total no. of Remediations
     * @param appliedRemediation Remediations that has been applied successfully
     * @param skippedRemediation Remediations that has been skipped
     * @param action Final action.
     * @return An ObjectNode representing the result.
     */

    public static ObjectNode buildResultNode(FoDReleaseDescriptor rd, int totalRemediation, int appliedRemediation, int skippedRemediation, String action) {
        ObjectNode result = rd.asObjectNode();
        result.put("releaseId", rd.getReleaseId());
        result.put("totalRemediation", totalRemediation);
        result.put("appliedRemediation", appliedRemediation);
        result.put("skippedRemediation", skippedRemediation);
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }
}
