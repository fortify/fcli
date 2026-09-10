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
package com.fortify.cli.aviator.ssc.helper;

/**
 * SSC application-version attribute definitions used by Aviator workflows.
 *
 * <p>These are SSC application-version attributes (not per-issue custom tags).
 * The definition is created by the {@code aviator ssc prepare} command and
 * the values are written by {@code aviator ssc correlate-sast-dast} and
 * {@code aviator ssc audit-dast}.
 */
public final class AviatorSSCAttributeDefinitions {

    private AviatorSSCAttributeDefinitions() {}

    /**
     * Descriptor for a custom SSC attribute definition managed by the Aviator module.
     *
     * @param name        Attribute name as it appears in SSC (used for lookup and write).
     * @param category    SSC attribute category (e.g. {@code "TECHNICAL"}).
     * @param type        SSC attribute type string (e.g. {@code "TEXT"}, {@code "DATE"}).
     * @param description Human-readable description stored in SSC.
     */
    public record AttributeDefinition(
        String name,
        String category,
        String type,
        String description
    ) {}

    /**
     * Free-text attribute written to an SSC application version after each
     * successful SAST-DAST correlation run.
     *
     * <p>Value is an ISO-8601 UTC timestamp produced by {@code Instant.now().toString()},
     * e.g. {@code 2026-04-30T14:32:00.123Z}. The bulk-correlation action reads this
     * attribute to decide whether an application version needs re-correlation.
     *
     * <p>TEXT type is used rather than DATE because SSC's DATE type only accepts
     * {@code yyyy-MM-dd}, which loses the time-of-day precision required for reliable
     * comparison with artifact {@code lastScanDate} values.
     */
    public static final AttributeDefinition LAST_CORRELATION_ATTR = new AttributeDefinition(
        "last_correlation",
        "TECHNICAL",
        "TEXT",
        "Timestamp of the last successful SAST-DAST correlation run (ISO-8601 UTC). Written by fcli aviator ssc correlate-sast-dast."
    );

    /**
     * Free-text attribute written after a DAST audit evaluation completes successfully.
     *
     * <p>The value is an ISO-8601 UTC timestamp. A successful evaluation includes a
     * run that finds no eligible findings, allowing bulk DAST audit selection to avoid
     * repeating a completed no-op evaluation until a newer DAST scan is available.
     */
    public static final AttributeDefinition LAST_DAST_AUDIT_ATTR = new AttributeDefinition(
        "last_dast_audit",
        "TECHNICAL",
        "TEXT",
        "Timestamp of the last successful DAST audit evaluation (ISO-8601 UTC). Written by fcli aviator ssc audit-dast."
    );
}
