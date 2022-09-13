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
package com.fortify.cli.common.rest.runner.config;

import lombok.Data;
import static com.fortify.cli.common.util.EnvHelper.*;

@Data
public final class UrlConfigFromEnv implements IUrlConfig {
    private final String urlEnvName;
    private final String proxyHostEnvName;
    private final String proxyPortEnvName;
    private final String proxyUserEnvName;
    private final String proxyPasswordEnvName;
    private final String insecureModeEnabledEnvName;
    private final String  url;
    private final String  proxyHost;
    private final Integer proxyPort;
    private final String  proxyUser;
    private final char[]  proxyPassword;
    private final boolean insecureModeEnabled;
    
    public UrlConfigFromEnv(String prefix) {
        this.urlEnvName = envName(prefix, "URL");
        this.proxyHostEnvName = envName(prefix, "PROXY_HOST");
        this.proxyPortEnvName = envName(prefix, "PROXY_PORT");
        this.proxyUserEnvName = envName(prefix, "PROXY_USER");
        this.proxyPasswordEnvName = envName(prefix, "PROXY_PASSWORD");
        this.insecureModeEnabledEnvName = envName(prefix, "DISABLE_SSL_CHECKS");
        this.url = env(urlEnvName);
        this.proxyHost = env(proxyHostEnvName);
        this.proxyPort = asInteger(env(proxyPortEnvName));
        this.proxyUser = env(proxyUserEnvName);
        this.proxyPassword = asCharArray(env(proxyPasswordEnvName));
        this.insecureModeEnabled = asBoolean(env(insecureModeEnabledEnvName));
        checkEnv();
    }
    
    public boolean hasConfigFromEnv() {
        return this.url != null;
    }
    
    private void checkEnv() {
        checkSecondaryWithoutPrimary(proxyHostEnvName, urlEnvName);
        checkSecondaryWithoutPrimary(proxyPortEnvName, proxyHostEnvName);
        checkSecondaryWithoutPrimary(proxyUserEnvName, proxyHostEnvName);
        checkSecondaryWithoutPrimary(proxyPasswordEnvName, proxyHostEnvName);
        checkSecondaryWithoutPrimary(insecureModeEnabledEnvName, urlEnvName);
    }
}
