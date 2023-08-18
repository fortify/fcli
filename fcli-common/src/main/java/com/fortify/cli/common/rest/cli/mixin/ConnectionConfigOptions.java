/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.rest.runner.config.IConnectionConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.Config;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure connection options to a remote system
 * </pre>
 * @author Ruud Senden
 */
@ReflectiveAccess
public abstract class ConnectionConfigOptions implements IConnectionConfig {
    private static final DateTimePeriodHelper periodHelper = DateTimePeriodHelper.byRange(Period.SECONDS, Period.MINUTES);
    
    @Option(names = {"--insecure", "-k"}, required = false, description = "Disable SSL checks", defaultValue = "false", order=6)
    @Getter private Boolean insecureModeEnabled;
    
    @Option(names = {"--socket-timeout"}, required = false, description = "Socket timeout for this session, for example 30s (30 seconds), 5m (5 minutes)", order=7)
    private String socketTimeout;
    
    @Option(names = {"--connect-timeout"}, required = false, description = "Connection timeout for this session in seconds, for example 30s (30 seconds), 5m (5 minutes)", order=8)
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
