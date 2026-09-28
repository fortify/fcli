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
package com.fortify.cli.aviator.fpr.remediation.model;

import com.fortify.cli.aviator.fpr.remediation.*;
import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;


public final class RequiredFields {

    private RequiredFields() {
    }

    public static String requireText(String value, String elementName) {
        if (value == null) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Missing required remediation element '" + elementName + "'");
        }
        return value;
    }

    public static int requireInt(String value, String elementName) {
        String text = requireText(value, elementName);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Invalid integer value for remediation element '" + elementName + "': " + text, e);
        }
    }

    public static int requireContextAttribute(String value, String attributeName) {
        if (value == null || value.isBlank()) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Missing required remediation context attribute '" + attributeName + "'");
        }
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue < 0) {
                throw new NumberFormatException("negative value");
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Invalid remediation context attribute '" + attributeName + "': " + value, e);
        }
    }
}
