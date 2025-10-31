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

import org.apache.commons.lang3.StringUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * This enum defines log message types. The log message type is recorded in
 * the fcli log file through {@link LogMessageTypeConverter} to allow for 
 * easy identification of log message types, and can be used for registering 
 * type-specific log masking values or patterns through {@link LogMaskHelper}.
 *
 * @author Ruud Senden
 */
public enum LogMessageType {
    FCLI, HTTP, HTTP_IN, HTTP_OUT, GRPC, GRPC_IN, GRPC_OUT, MCP, OTHER;
    
    private final String fixedLengthString;
    /** Constructor to generate a fixed length string for the current
     *  log message type. If new types are added, the right padding
     *  amount may need to be increased. */
    LogMessageType() {
        this.fixedLengthString = StringUtils.rightPad(this.name(), 8);
    }
    
    /**
     * Return the fixed length string for the current log message type.
     */
    public final String toFixedLengthString() {
        return fixedLengthString;
    }
    
    /**
     * Return all known log message types. This is just a synonym for
     * {@link #values()} for semantic reasons.
     */
    public static final LogMessageType[] all() { return values(); }
    
    /**
     * Get the log message type for the given {@link ILoggingEvent}.
     */
    public static final LogMessageType getType(ILoggingEvent event) {
        var loggerName = event.getLoggerName();
        if ( loggerName!=null ) {
            if ( loggerName.startsWith("com.fortify.cli") ) {
                return LogMessageType.FCLI;
            } else if ( loggerName.startsWith("org.apache.http") ) {
                if ( loggerName.endsWith("headers") || loggerName.endsWith("wire") ) {
                    return StringUtils.substringAfter(event.getFormattedMessage(), " ").startsWith("<<") 
                        ? LogMessageType.HTTP_IN : LogMessageType.HTTP_OUT;
                } else {
                    return LogMessageType.HTTP;
                }
            } else if ( loggerName.endsWith("NettyClientHandler") ) {
                var msg = event.getFormattedMessage();
                if ( msg.contains("INBOUND") ) { return LogMessageType.GRPC_IN; }
                else if ( msg.contains("OUTBOUND") ) { return LogMessageType.GRPC_OUT; }
                else { return LogMessageType.GRPC; }
            } else if ( loggerName.startsWith("io.modelcontextprotocol") ) {
                return LogMessageType.MCP;
            }
        }
        return LogMessageType.OTHER;
    }
}