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
package com.fortify.cli.common.fpr.util;

/** Constants shared by the product-neutral FPR parsing code. */
public final class FprConstants {
    private FprConstants() {}

    public static final String PENDING_REVIEW = "Pending Review/Not Set";

    // Standard audit tag ids
    public static final String ANALYSIS_TAG_ID = "87f2364f-dcd4-49e6-861d-f8d3f351686b";
    public static final String AUDITOR_STATUS_TAG_ID = "ACB05E55-E74D-468C-8501-52E1FDC27D71";
    public static final String FOD_TAG_ID = "604f0fbe-b5fe-47cd-a9cb-587ad8ebe93a";

    // File size protection — prevent zip bomb and decompression DOS attacks
    public static final long MAX_SOURCE_FILE_SIZE = 50L * 1024L * 1024L; // 50 MB
}
