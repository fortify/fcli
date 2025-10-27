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
package com.fortify.cli.aviator.audit.model;


public enum Priority {
    Critical,
    High,
    Medium,
    Low;

    public static Priority fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Priority value cannot be null.");
        }
        for (Priority p : values()) {
            if (p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("No Priority constant with name '" + value + "' found.");
    }
}