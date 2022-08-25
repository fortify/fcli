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
package com.fortify.cli.fod.session.manager;

import com.fortify.cli.common.rest.runner.IConnectionConfig;
import com.fortify.cli.common.rest.runner.IConnectionConfigProvider;

import io.micronaut.core.util.StringUtils;
import lombok.Data;

@Data
public class FoDSessionLoginConfig implements IConnectionConfigProvider {
    private IConnectionConfig connectionConfig;
    private IFoDUserCredentialsConfig fodUserCredentialsConfig;
    private IFoDClientCredentialsConfig fodClientCredentialsConfig;
    private String[] scopes = {"api-tenant"};
    private boolean renewAllowed;

    public final boolean hasUserCredentialsConfig() {
        return fodUserCredentialsConfig!=null 
                && StringUtils.isNotEmpty(fodUserCredentialsConfig.getUser())
                && fodUserCredentialsConfig.getPassword()!=null;
    }
    
    public final boolean hasClientCredentials() {
        return fodClientCredentialsConfig!=null
                && StringUtils.isNotEmpty(fodClientCredentialsConfig.getClientId())
                && StringUtils.isNotEmpty(fodClientCredentialsConfig.getClientSecret());
    }
}
