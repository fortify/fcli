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
package com.fortify.cli.common.spel.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class SpelFunctionsStandardDateTest {
    @Test
    void parsesIsoInstantAndDateOnly() {
        assertEquals(OffsetDateTime.parse("2026-04-30T14:32:00.123456789Z"),
            SpelFunctionsStandard.date("2026-04-30T14:32:00.123456789Z"));
        assertEquals(LocalDate.parse("2026-04-30").atStartOfDay().atOffset(ZoneOffset.UTC),
            SpelFunctionsStandard.date("2026-04-30"));
        assertNull(SpelFunctionsStandard.date(null));
    }

    @Test
    void parsesSscOffsetWithoutColon() {
        OffsetDateTime parsed = SpelFunctionsStandard.date("2026-04-30T14:32:00.000+0000");

        assertEquals(OffsetDateTime.parse("2026-04-30T14:32:00Z"), parsed);
        assertFalse(parsed.isAfter(SpelFunctionsStandard.date("2026-04-30T14:32:00.000Z")));
        assertTrue(SpelFunctionsStandard.date("2026-05-01T00:00:00.000+0000")
            .isAfter(SpelFunctionsStandard.date("2026-04-30T14:32:00.000Z")));
    }

    @Test
    void rejectsTextThatIsNotADate() {
        assertThrows(DateTimeParseException.class, () -> SpelFunctionsStandard.date("not-a-date"));
    }
}
