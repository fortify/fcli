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

import java.util.UUID;

public class StringUtil {

    private StringUtil() {
    }

    public static boolean isEmpty(String test) {
        return test == null || test.length() == 0;
    }

    public static boolean isPendingReviewValue(String value) {
        if (value == null) {
            return true;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty()
                || "Pending Review".equalsIgnoreCase(normalizedValue)
                || "Not Set".equalsIgnoreCase(normalizedValue)
                || FprConstants.PENDING_REVIEW.equalsIgnoreCase(normalizedValue);
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
        if (description.indexOf('<') < 0) {
            return description.trim();
        }

        // Literal replace: replaceAll would compile a regex on every call.
        description = description.replace("<pre>", "<code>");
        description = description.replace("</pre>", "</code>");
        description = description.replace("<p>", "");
        description = description.replace("</p>", "\n");
        description = description.replace("<table>", "");
        description = description.replace("</table>", "");
        description = description.replace("<tr>", "");
        description = description.replace("</tr>", "");
        description = description.replace("<td>", "\t");
        description = description.replace("</td>", "");
        description = description.replace("<th>", "<b>\t");
        description = description.replace("</th>", "</b>");
        description = description.replace("<li>", "-");
        description = description.replace("</li>", "");
        description = description.replace("<blockquote>", "");
        description = description.replace("</blockquote>", "");
        if (stripMarkupTags) {
            description = description.replace("<b>", "");
            description = description.replace("</b>", "");
            description = description.replace("<code>", "");
            description = description.replace("</code>", "");
            description = description.replace("<h1>", "");
            description = description.replace("</h1>", "");
            description = description.replace("<ul>", "");
            description = description.replace("</ul>", "");
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
