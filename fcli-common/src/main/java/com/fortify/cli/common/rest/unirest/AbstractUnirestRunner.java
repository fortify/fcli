/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.common.rest.unirest;

import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.rest.data.BasicConnectionConfig;
import com.fortify.cli.common.rest.data.IBasicConnectionConfigProvider;
import com.fortify.cli.common.session.LoginSessionHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.Getter;

// TODO For now this class instantiates a new UnirestInstance on every call to runWithUnirest,
//      which should be OK when running individual commands but less performant when running
//      multiple commands in a composite command or workflow.
@ReflectiveAccess
public abstract class AbstractUnirestRunner<D> implements IUnirestRunner {
	@Getter @Inject private ObjectMapper objectMapper;
	@Getter @Inject private LoginSessionHelper loginSessionHelper;
	
	private final UnirestInstance createUnirestInstance() {
		UnirestInstance instance = Unirest.spawnInstance();
		instance.config().setObjectMapper(new JacksonObjectMapper(objectMapper));
		return instance;
	}
	
	/**
	 * Run the given runner with a {@link UnirestInstance} that has been configured
	 * based on the given login session data.
	 * @param <R> Return type
	 * @param loginSessionName for this session
	 * @param loginSessionData to be used to configure the {@link UnirestInstance}
	 * @param runner to perform the actual work with a configured {@link UnirestInstance}
	 * @return Return value of runner; note that this return value shouldn't contain any reference to the 
	 *         {@link UnirestInstance} as that might be closed once this call returns.
	 */
	public <R> R runWithUnirest(String loginSessionName, D loginSessionData, Function<UnirestInstance, R> runner) {
		if ( loginSessionData == null ) {
			throw new IllegalStateException("Login session data may not be null");
		}
		try ( var unirestInstance = createUnirestInstance() ) {
			_configure(loginSessionName, loginSessionData, unirestInstance);
			return runner.apply(unirestInstance);
		}
	}
	
	/**
	 * Run the given runner with a {@link UnirestInstance} that has been configured
	 * based on the given login session name.
	 * @param <R> Return type
	 * @param loginSessionName used to get the login session data used to configure the {@link UnirestInstance}
	 * @param runner to perform the actual work with a configured {@link UnirestInstance}
	 * @return Return value of runner; note that this return value shouldn't contain any reference to the 
	 *         {@link UnirestInstance} as that might be closed once this call returns.
	 */
	@Override
	public <R> R runWithUnirest(String loginSessionName, Function<UnirestInstance, R> runner) {
		D loginSessionData = getLoginSessionData(loginSessionName);
		if ( loginSessionData==null ) { 
			throw new IllegalStateException("No active login session "+loginSessionName+" found for "+getLoginSessionType()); 
		}
		return runWithUnirest(loginSessionName, loginSessionData, runner);
	}
	
	/**
	 * Get the login session data for the given login session name
	 * @param loginSessionName for which to get the login session data
	 * @return login session data
	 */
	protected D getLoginSessionData(String loginSessionName) {
		return loginSessionHelper.getData(getLoginSessionType(), loginSessionName, getLoginSessionDataClass());
	}

	/**
	 * Perform basic connection configuration if the given login session data implements
	 * {@link IBasicConnectionConfigProvider}. Afterwards the {@link #configure(String, Object, UnirestInstance)}
	 * method is called to allow subclasses to perform any additional configuration, like setting
	 * authentication headers.
	 * @param loginSessionData used to configure the {@link UnirestInstance}
	 * @param unirestInstance {@link UnirestInstance} to be configured
	 */
	private final void _configure(String loginSessionName, D loginSessionData, UnirestInstance unirestInstance) {
		if ( loginSessionData instanceof IBasicConnectionConfigProvider ) {
			IBasicConnectionConfigProvider csp = (IBasicConnectionConfigProvider)loginSessionData;
			BasicConnectionConfig cs = csp.getBasicConnectionConfig();
			if ( cs == null ) { throw new IllegalArgumentException("Connection configuration may not be null"); }
			unirestInstance.config()
				.defaultBaseUrl(cs.getUrl())
				.verifySsl(cs.isInsecureModeEnabled());
			if ( StringUtils.isNotEmpty(cs.getProxyHost()) ) {
				unirestInstance.config().proxy(cs.getProxyHost(), cs.getProxyPort(), cs.getProxyUser(), 
						cs.getProxyHost()==null ? null : String.valueOf(cs.getProxyPassword()));
			}
		}
		configure(loginSessionName, loginSessionData, unirestInstance);
	}
	
	/**
	 * Subclasses must implement this method to perform any additional configuration of the given
	 * {@link UnirestInstance} based on the given login session data.
	 * @param loginSessionData used to configure the {@link UnirestInstance}
	 * @param unirestInstance {@link UnirestInstance} to be configured
	 */
	protected abstract void configure(String loginSessionName, D loginSessionData, UnirestInstance unirestInstance);
	
	/**
	 * Subclasses must implement this method to return the login session type, which is usually
	 * a short identifier describing the target system, like 'ssc' or 'fod'. 
	 * @return login session type
	 */
	protected abstract String getLoginSessionType();
	
	/**
	 * Subclasses must implement this method to return the login session data class
	 * @return login session data class
	 */
	protected abstract Class<D> getLoginSessionDataClass();
}
