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

import java.util.Collections;
import java.util.Set;

import lombok.Getter;

/**
 * This enum defines log mask levels that can be applied to log messages. The log mask level
 * to be applied is defined through the generic fcli <pre>--log-mask</pre> option. Each log
 * mask level is mapped to one or more {@link LogSensitivityLevel} values, to identify which
 * sensitivity levels should be masked for a given log mask level. 
 *
 * @author Ruud Senden
 */
public enum LogMaskLevel {
    high(LogSensitivityLevel.high, LogSensitivityLevel.medium, LogSensitivityLevel.low), 
    medium(LogSensitivityLevel.high, LogSensitivityLevel.medium), 
    low(LogSensitivityLevel.high), 
    none;
    
    @Getter private final Set<LogSensitivityLevel> sensitivityLevels;
    private LogMaskLevel(LogSensitivityLevel... sensitivityLevels) {
        this.sensitivityLevels = sensitivityLevels==null ? Collections.emptySet() : Set.of(sensitivityLevels);
    }
}