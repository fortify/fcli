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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class StringUtilTest {
    private static final String DESCRIPTION = "  <p>Intro $1 \\d</p><pre>code()</pre>"
            + "<table><tr><th>H</th></tr><tr><td>C</td></tr></table>"
            + "<ul><li>item</li></ul><blockquote>q</blockquote><h1>T</h1><i>kept</i>  ";

    @Test
    public void testStripTagsKeepsMarkupTagsWhenNotRequested() {
        assertEquals("Intro $1 \\d\n<code>code()</code><b>\tH</b>\tC<ul>-item</ul>q<h1>T</h1><i>kept</i>",
                StringUtil.stripTags(DESCRIPTION, false));
    }

    @Test
    public void testStripTagsRemovesMarkupTagsWhenRequested() {
        assertEquals("Intro $1 \\d\ncode()\tH\tC-itemqT<i>kept</i>",
                StringUtil.stripTags(DESCRIPTION, true));
    }

    @Test
    public void testStripTagsHandlesNullAndPlainText() {
        assertEquals("", StringUtil.stripTags(null, true));
        assertEquals("plain $text", StringUtil.stripTags("  plain $text \n", true));
    }
}
