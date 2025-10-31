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
package com.fortify.cli.common.log;

/**
 * This enum defines sensitivity levels for values being logged, that may need to be masked 
 * based on current {@link LogMaskLevel}.
 *
 * @author Ruud Senden
 */
public enum LogSensitivityLevel {
    /** High sensitivity data, like tokens, passwords & other credentials */
    high, 
    /** Medium sensitivity data, like current user name */
    medium, 
    /** Low sensitivity data, like URLs, tenants, ... */
    low;
}