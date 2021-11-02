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
package com.fortify.cli.fod.rest.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fortify.cli.common.rest.data.BasicConnectionConfig;
import com.fortify.cli.common.rest.data.IBasicConnectionConfigProvider;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.util.StringUtils;
import lombok.Data;

@Introspected @Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class FoDConnectionConfig implements IBasicConnectionConfigProvider {
	private BasicConnectionConfig basicConnectionConfig;
	private FoDUserCredentialsConfig fodUserCredentialsConfig;
	private FoDClientCredentialsConfig fodClientCredentialsConfig;
	private String[] scopes = {"api-tenant"};
	private boolean renewAllowed;

	@JsonIgnore public final boolean hasUserCredentialsConfig() {
		return fodUserCredentialsConfig!=null 
				&& StringUtils.isNotEmpty(fodUserCredentialsConfig.getUser())
				&& fodUserCredentialsConfig.getPassword()!=null;
	}
	
	@JsonIgnore public final boolean hasClientCredentials() {
		return fodClientCredentialsConfig!=null
				&& StringUtils.isNotEmpty(fodClientCredentialsConfig.getClientId())
				&& StringUtils.isNotEmpty(fodClientCredentialsConfig.getClientSecret());
	}
	
	@JsonIgnore public final BasicConnectionConfig getNonNullBasicConnectionConfig() {
		basicConnectionConfig = basicConnectionConfig==null ? new BasicConnectionConfig() : basicConnectionConfig;
		return basicConnectionConfig;
	}
	
	@JsonIgnore public final FoDUserCredentialsConfig getNonNullUserCredentialsConfig() {
		fodUserCredentialsConfig = fodUserCredentialsConfig==null ? new FoDUserCredentialsConfig() : fodUserCredentialsConfig;
		return fodUserCredentialsConfig;
	}
	
	@JsonIgnore public final FoDClientCredentialsConfig getNonNullClientCredentialsConfig() {
		fodClientCredentialsConfig = fodClientCredentialsConfig==null ? new FoDClientCredentialsConfig() : fodClientCredentialsConfig;
		return fodClientCredentialsConfig;
	}

	
}
