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
package com.fortify.cli.common.rest.cli.mixin;

import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.unirest.config.IConnectionConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import kong.unirest.Config;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure connection options to a remote system
 * </pre>
 * @author Ruud Senden
 */
public abstract class ConnectionConfigOptions implements IConnectionConfig {
    private static final DateTimePeriodHelper periodHelper = DateTimePeriodHelper.byRange(Period.SECONDS, Period.MINUTES);
    
    @DisableTest({TestType.MULTI_OPT_PLURAL_NAME, TestType.MULTI_OPT_SPLIT, TestType.OPT_SHORT_NAME})
    @Option(names = {"--header", "-H"}, required = false, order=6)
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "HEADER VALUE", pattern = "[^:]+:\\s*(.+)")
    @Getter private List<String> headers = new ArrayList<>();
    
    @Option(names = {"--insecure", "-k"}, required = false, defaultValue = "false", order=7)
    @Getter private Boolean insecureModeEnabled;
    
    @Option(names = {"--socket-timeout"}, required = false, order=8)
    private String socketTimeout;
    
    @Option(names = {"--connect-timeout"}, required = false, order=9)
    private String connectTimeout;
    
    @Override
    public int getConnectTimeoutInMillis() {
        return connectTimeout==null ? getDefaultConnectTimeoutInMillis() : (int)periodHelper.parsePeriodToMillis(connectTimeout);
    }
    
    @Override
    public int getSocketTimeoutInMillis() {
        return socketTimeout==null ? getDefaultSocketTimeoutInMillis() : (int)periodHelper.parsePeriodToMillis(socketTimeout);
    }
    
    protected int getDefaultSocketTimeoutInMillis() {
        return Config.DEFAULT_SOCKET_TIMEOUT;
    }
    
    protected int getDefaultConnectTimeoutInMillis() {
        return Config.DEFAULT_CONNECT_TIMEOUT;
    }
}
