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
package com.fortify.cli.common.rest.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;

public class FetchRangeTest {
    private final FetchRangeConverter converter = new FetchRangeConverter();

    @Test
    void endOnly() {
        var result = converter.convert("10");
        assertEquals(0, result.offset());
        assertEquals(10, result.limit());
    }

    @Test
    void startAndEndFromOne() {
        var result = converter.convert("1-10");
        assertEquals(0, result.offset());
        assertEquals(10, result.limit());
    }

    @Test
    void startAndEndMidRange() {
        var result = converter.convert("21-30");
        assertEquals(20, result.offset());
        assertEquals(10, result.limit());
    }

    @Test
    void singleRecord() {
        var result = converter.convert("1");
        assertEquals(0, result.offset());
        assertEquals(1, result.limit());
    }

    @Test
    void zeroEndRejected() {
        var ex = assertThrows(FcliSimpleException.class, () -> converter.convert("0"));
        assertEquals("--fetch value must be >= 1", ex.getMessage());
    }

    @Test
    void zeroStartRejected() {
        var ex = assertThrows(FcliSimpleException.class, () -> converter.convert("0-10"));
        assertEquals("--fetch start must be >= 1", ex.getMessage());
    }

    @Test
    void endBeforeStartRejected() {
        var ex = assertThrows(FcliSimpleException.class, () -> converter.convert("30-21"));
        assertEquals("--fetch end must be >= start (30)", ex.getMessage());
    }

    @Test
    void nonNumericRejected() {
        var ex = assertThrows(FcliSimpleException.class, () -> converter.convert("abc"));
        assertEquals("Invalid --fetch value 'abc'. Expected format: [<start>-]<end>", ex.getMessage());
    }

    @Test
    void extraSeparatorRejected() {
        var ex = assertThrows(FcliSimpleException.class, () -> converter.convert("1-2-3"));
        assertEquals("Invalid --fetch value '1-2-3'. Expected format: [<start>-]<end>", ex.getMessage());
    }
}
