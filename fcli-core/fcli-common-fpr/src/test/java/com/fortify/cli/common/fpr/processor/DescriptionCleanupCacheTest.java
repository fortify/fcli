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
package com.fortify.cli.common.fpr.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.fpr.util.StringUtil;

class DescriptionCleanupCacheTest {
    @Test
    void reusesEqualInputsAndKeepsDifferentInputsSeparate() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache();
        String first = cache.clean("<b>first</b>");
        assertEquals("first", first);
        assertSame(first, cache.clean(new String("<b>first</b>")));
        assertEquals("second", cache.clean("<b>second</b>"));
        assertSame(first, cache.clean("<b>first</b>"));
    }

    @Test
    void preservesNullPlainTextWhitespaceAndEmptyResults() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache();
        for (String input : new String[] {null, "", "  plain\r\n", "\u00a0text\u00a0", "<b></b>", "<unknown>"}) {
            assertEquals(StringUtil.stripTags(input, true), cache.clean(input));
            assertEquals(StringUtil.stripTags(input, true), cache.clean(input));
        }
    }

    @Test
    void evictsLeastRecentlyUsedEntryAtEntryLimit() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache(2, 1_000);
        String first = cache.clean("<b>first</b>");
        String second = cache.clean("<b>second</b>");
        assertSame(first, cache.clean("<b>first</b>"));
        cache.clean("<b>third</b>");
        assertSame(first, cache.clean("<b>first</b>"));
        String reloaded = cache.clean("<b>second</b>");
        assertEquals(second, reloaded);
        assertNotSame(second, reloaded);
    }

    @Test
    void boundsCombinedInputAndOutputCharacters() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache(10, 40);
        String first = cache.clean("<b>aaaaaaaa</b>"); // 15 input + 8 output characters.
        String second = cache.clean("<b>bbbbbbbb</b>");
        assertSame(second, cache.clean("<b>bbbbbbbb</b>"));
        String reloaded = cache.clean("<b>aaaaaaaa</b>");
        assertEquals(first, reloaded);
        assertNotSame(first, reloaded);
    }

    @Test
    void bypassesOversizedEntriesWithoutEvictingUsefulResults() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache(2, 40);
        String first = cache.clean("<b>first</b>");
        for (int length : new int[] {20, 50}) {
            String input = "<b>" + "x".repeat(length) + "</b>";
            String cleaned = cache.clean(input);
            assertEquals(StringUtil.stripTags(input, true), cleaned);
            assertNotSame(cleaned, cache.clean(input));
            assertSame(first, cache.clean("<b>first</b>"));
        }
    }

    @Test
    void clearsEntriesAndCharacterAccounting() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache(2, 40);
        String first = cache.clean("<b>aaaaaaaa</b>");
        cache.clear();
        String reloaded = cache.clean("<b>aaaaaaaa</b>");
        assertEquals(first, reloaded);
        assertNotSame(first, reloaded);
        assertSame(reloaded, cache.clean("<b>aaaaaaaa</b>"));
        DescriptionCleanupCache other = new DescriptionCleanupCache();
        assertNotSame(reloaded, other.clean("<b>aaaaaaaa</b>"));
    }

    @Test
    void matchesExistingCleanupAcrossTagCombinationsAndEvictions() {
        DescriptionCleanupCache cache = new DescriptionCleanupCache(8, 512);
        String[] tokens = {"<pre>", "</pre>", "<p>", "</p>", "<table>", "</table>", "<tr>", "</tr>",
            "<td>", "</td>", "<th>", "</th>", "<li>", "</li>", "<blockquote>", "</blockquote>",
            "<b>", "</b>", "<code>", "</code>", "<h1>", "</h1>", "<ul>", "</ul>", "<unknown>",
            "<", ">", "b>", "$1\\d", "\r\n", "\t", "\u2028", "\u00a0", "\u03bb\ud83d\ude00", "text", " "};
        Random random = new Random(314159);
        for (int sample = 0; sample < 5_000; sample++) {
            StringBuilder input = new StringBuilder();
            int length = random.nextInt(50);
            for (int token = 0; token < length; token++) {
                input.append(tokens[random.nextInt(tokens.length)]);
            }
            String text = input.toString();
            String expected = StringUtil.stripTags(text, true);
            assertEquals(expected, cache.clean(text), "Cache miss: " + sample);
            assertEquals(expected, cache.clean(text), "Repeated input: " + sample);
        }
    }

    @Test
    void rejectsNonpositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> new DescriptionCleanupCache(0, 40));
        assertThrows(IllegalArgumentException.class, () -> new DescriptionCleanupCache(2, 0));
    }
}
