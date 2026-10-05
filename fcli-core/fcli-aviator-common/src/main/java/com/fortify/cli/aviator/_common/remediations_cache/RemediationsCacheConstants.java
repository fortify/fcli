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
package com.fortify.cli.aviator._common.remediations_cache;

public final class RemediationsCacheConstants {
    public static final int SCHEMA_VERSION = 1;
    public static final String KIND = "aviator-remediations-cache";
    public static final String MANIFEST_ENTRY = "manifest.json";
    public static final String FPRS_DIR = "fprs";
    public static final String PRODUCT_SSC = "ssc";
    public static final String PRODUCT_FOD = "fod";

    private RemediationsCacheConstants() {}
}
