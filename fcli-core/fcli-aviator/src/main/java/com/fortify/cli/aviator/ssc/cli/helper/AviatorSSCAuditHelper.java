package com.fortify.cli.aviator.ssc.cli.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

/**
 * Helper class for the AviatorSSCAuditCommand to encapsulate
 * result message formatting and JSON output construction.
 */
public final class AviatorSSCAuditHelper {
    private AviatorSSCAuditHelper() {}

    /**
     * Builds the final JSON result node for the command output.
     * @param av The SSCAppVersionDescriptor.
     * @param artifactId The ID of the uploaded artifact, or a status string.
     * @param action The final action string for the output.
     * @return An ObjectNode representing the result.
     */
    public static ObjectNode buildResultNode(SSCAppVersionDescriptor av, String artifactId, String action) {
        ObjectNode result = av.asObjectNode();
        result.put("id", av.getVersionId());
        result.put("applicationName", av.getApplicationName());
        result.put("name", av.getVersionName());
        result.put("artifactId", artifactId);
        result.put(IActionCommandResultSupplier.actionFieldName, action);
        return result;
    }

    /**
     * Generates a detailed action string based on the FPRAuditResult.
     * This is used for the 'action' column in the output.
     * @param auditResult The result from the Aviator audit.
     * @return A descriptive string of the outcome.
     */
    public static String getDetailedAction(FPRAuditResult auditResult) {
        switch (auditResult.getStatus()) {
            case "SKIPPED":
                String reason = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown reason";
                return "SKIPPED (" + reason + ")";
            case "FAILED":
                String message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown error";
                return "FAILED (" + message + ")";
            case "PARTIALLY_AUDITED":
                return String.format("PARTIALLY_AUDITED (%d/%d audited)",
                        auditResult.getIssuesSuccessfullyAudited(),
                        auditResult.getTotalIssuesToAudit());
            case "AUDITED":
                return "AUDITED";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Generates a progress message for the user based on the FPRAuditResult.
     * @param auditResult The result from the Aviator audit.
     * @return A user-friendly progress message.
     */
    public static String getProgressMessage(FPRAuditResult auditResult) {
        switch (auditResult.getStatus()) {
            case "SKIPPED":
                return "No issues to audit, skipping upload";
            case "FAILED":
                String message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown error";
                return "Audit failed: " + message;
            case "PARTIALLY_AUDITED":
            case "AUDITED":
                return auditResult.getUpdatedFile() != null
                        ? "Status: Uploading audited FPR to SSC"
                        : "Audit completed but no file updated";
            default:
                return "Unknown audit status";
        }
    }
}
