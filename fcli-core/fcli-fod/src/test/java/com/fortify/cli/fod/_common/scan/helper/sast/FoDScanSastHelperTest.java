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
package com.fortify.cli.fod._common.scan.helper.sast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FoDScanSastHelperTest {
    @Test
    void normalizeSetupScanPolicy() {
        assertEquals("FoD_Legacy", FoDScanSastHelper.normalizeSetupScanPolicy("legacy"));
        assertEquals("Security", FoDScanSastHelper.normalizeSetupScanPolicy("security"));
        assertEquals("DevOps", FoDScanSastHelper.normalizeSetupScanPolicy("devops"));
        assertEquals("Classic", FoDScanSastHelper.normalizeSetupScanPolicy("classic"));
        assertEquals("FuturePolicy", FoDScanSastHelper.normalizeSetupScanPolicy(" FuturePolicy "));
        assertNull(FoDScanSastHelper.normalizeSetupScanPolicy(" "));
    }

    @Test
    void normalizeStartScanPolicy() {
        assertEquals("FoD_Legacy_", FoDScanSastHelper.normalizeStartScanPolicy("legacy"));
        assertEquals("FoD_Legacy_", FoDScanSastHelper.normalizeStartScanPolicy("FoD_Legacy"));
        assertEquals("Security", FoDScanSastHelper.normalizeStartScanPolicy("security"));
        assertEquals("DevOps", FoDScanSastHelper.normalizeStartScanPolicy("devops"));
        assertEquals("Classic", FoDScanSastHelper.normalizeStartScanPolicy("classic"));
        assertEquals("FuturePolicy", FoDScanSastHelper.normalizeStartScanPolicy(" FuturePolicy "));
        assertNull(FoDScanSastHelper.normalizeStartScanPolicy(null));
    }
}