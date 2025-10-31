/*
 * Copyright 2021-2025 Open Text.
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

import java.util.UUID;

public class StringUtil {

    private StringUtil() {
    }

    public static boolean isEmpty(String test) {
        return test == null || test.length() == 0;
    }

    /**
     * Strips HTML-like tags from a string to clean it up for display.
     *
     * @param description     The string containing tags.
     * @param stripMarkupTags If true, strips basic formatting tags like <b> and <code>.
     * @return The cleaned string.
     */
    public static String stripTags(String description, boolean stripMarkupTags) {
        if (description == null) {
            return "";
        }

        description = description.replaceAll("<pre>", "<code>");
        description = description.replaceAll("</pre>", "</code>");
        description = description.replaceAll("<p>", "");
        description = description.replaceAll("</p>", "\n");
        description = description.replaceAll("<table>", "");
        description = description.replaceAll("</table>", "");
        description = description.replaceAll("<tr>", "");
        description = description.replaceAll("</tr>", "");
        description = description.replaceAll("<td>", "\t");
        description = description.replaceAll("</td>", "");
        description = description.replaceAll("<th>", "<b>\t");
        description = description.replaceAll("</th>", "</b>");
        description = description.replaceAll("<li>", "-");
        description = description.replaceAll("</li>", "");
        description = description.replaceAll("<blockquote>", "");
        description = description.replaceAll("</blockquote>", "");
        description = description.replaceAll(" ", " ");
        if (stripMarkupTags) {
            description = description.replaceAll("<b>", "");
            description = description.replaceAll("</b>", "");
            description = description.replaceAll("<code>", "");
            description = description.replaceAll("</code>", "");
            description = description.replaceAll("<h1>", "");
            description = description.replaceAll("</h1>", "");
            description = description.replaceAll("<ul>", "");
            description = description.replaceAll("</ul>", "");
        }
        return description.trim();
    }

    public static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }

        if (str.length() != 36) {
            return false;
        }

        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
