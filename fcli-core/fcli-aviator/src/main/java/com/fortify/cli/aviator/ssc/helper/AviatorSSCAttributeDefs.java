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

public final class AviatorSSCAttributeDefs {

    private AviatorSSCAttributeDefs() {}

    /**
     * Descriptor for a custom SSC attribute definition managed by the Aviator module.
     *
     * @param guid        Fixed GUID — must never change once deployed to an SSC instance.
     * @param name        Attribute name as it appears in SSC (used for lookup and write).
     * @param category    SSC attribute category (e.g. {@code "Technical"}).
     * @param type        SSC attribute type string (e.g. {@code "TEXT"}, {@code "DATE"}).
     * @param description Human-readable description stored in SSC.
     */
    public record AttributeDefinition(
        String guid,
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
        "B2C3D4E5-F6A7-8901-BCDE-F12345678901",  // not sent on POST; used only for reference
        "last_correlation",
        "TECHNICAL",   // must be UPPERCASE — SSC rejects "Technical" (HTTP 400)
        "TEXT",
        "Timestamp of the last successful SAST-DAST correlation run (ISO-8601 UTC). Written by fcli aviator ssc correlate-sast-dast."
    );
}
