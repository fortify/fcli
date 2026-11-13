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
package com.fortify.cli.aviator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void aviatorStatusValuesContainBothStatusesInOrder() {
        assertEquals(List.of(Constants.PROCESSED_BY_AVIATOR, Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION),
            Constants.AVIATOR_STATUS_VALUES);
    }

    @Test
    void isAviatorStatusRecognizesBothStatusesIgnoringCase() {
        assertTrue(Constants.isAviatorStatus(Constants.PROCESSED_BY_AVIATOR));
        assertTrue(Constants.isAviatorStatus(Constants.PROCESSED_BY_AVIATOR_WITH_REMEDIATION));
        assertTrue(Constants.isAviatorStatus("processed_by_aviator_with_remediation"));
    }

    @Test
    void isAviatorStatusRejectsOtherValues() {
        assertFalse(Constants.isAviatorStatus(null));
        assertFalse(Constants.isAviatorStatus(""));
        assertFalse(Constants.isAviatorStatus("NOT_PROCESSED"));
    }
}
