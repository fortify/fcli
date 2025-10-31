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

import com.formkiq.graalvm.annotations.Reflectable;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * This class provides the 'msgType' conversion word that can be 
 * used in logging pattern layouts to show the {@link LogMessageType}
 * in log entries.
 * 
 * @author Ruud Senden
 */
@Reflectable
public class LogMessageTypeConverter extends ClassicConverter {
    public static final String conversionWord = "msgType";
    @Override
    public String convert(ILoggingEvent event) {
        return LogMessageType.getType(event).toFixedLengthString();
    }
}
