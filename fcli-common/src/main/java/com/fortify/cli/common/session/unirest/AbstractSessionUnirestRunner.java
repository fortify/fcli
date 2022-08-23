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
package com.fortify.cli.common.session.unirest;

import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.rest.unirest.runner.ConnectionConfigUnirestRunner;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.api.SessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// TODO For now this class instantiates a new UnirestInstance on every call to runWithUnirest,
//      which should be OK when running individual commands but less performant when running
//      multiple commands in a composite command or workflow.
@ReflectiveAccess
public abstract class AbstractSessionUnirestRunner<D extends ISessionData> {
	@Getter @Inject private ConnectionConfigUnirestRunner unirestRunner;
	@Getter @Inject private ObjectMapper objectMapper;
	@Getter @Inject private SessionDataManager authSessionPersistenceHelper;
	
	/**
	 * Run the given runner with a {@link UnirestInstance} that has been configured
	 * based on the given login session data.
	 * @param <R> Return type
	 * @param authSessionName for this session
	 * @param authSessionData to be used to configure the {@link UnirestInstance}
	 * @param runner to perform the actual work with a configured {@link UnirestInstance}
	 * @return Return value of runner; note that this return value shouldn't contain any reference to the 
	 *         {@link UnirestInstance} as that might be closed once this call returns.
	 */
	public <R> R runWithUnirest(String authSessionName, D authSessionData, Function<UnirestInstance, R> runner) {
		if ( authSessionData == null ) {
			throw new IllegalStateException("Login session data may not be null");
		}
		return unirestRunner.runWithUnirest(authSessionData.getConnectionConfig(), unirest -> {
			configure(authSessionName, authSessionData, unirest);
			return runner.apply(unirest);
		});
	}
	
	/**
	 * Run the given runner with a {@link UnirestInstance} that has been configured
	 * based on the given login session name.
	 * @param <R> Return type
	 * @param authSessionName used to get the login session data used to configure the {@link UnirestInstance}
	 * @param runner to perform the actual work with a configured {@link UnirestInstance}
	 * @return Return value of runner; note that this return value shouldn't contain any reference to the 
	 *         {@link UnirestInstance} as that might be closed once this call returns.
	 */
	public <R> R runWithUnirest(String authSessionName, Function<UnirestInstance, R> runner) {
		D authSessionData = getAuthSessionData(authSessionName);
		if ( authSessionData==null ) { 
			throw new IllegalStateException("No active login session "+authSessionName+" found for "+getSessionType()); 
		}
		return runWithUnirest(authSessionName, authSessionData, runner);
	}
	
	/**
	 * Get the login session data for the given login session name
	 * @param authSessionName for which to get the login session data
	 * @return login session data
	 */
	protected D getAuthSessionData(String authSessionName) {
		return authSessionPersistenceHelper.getData(getSessionType(), authSessionName, getSessionDataClass());
	}

	/**
	 * Subclasses must implement this method to perform any additional configuration of the given
	 * {@link UnirestInstance} based on the given login session data.
	 * @param authSessionData used to configure the {@link UnirestInstance}
	 * @param unirestInstance {@link UnirestInstance} to be configured
	 */
	protected abstract void configure(String authSessionName, D authSessionData, UnirestInstance unirestInstance);
	
	/**
	 * Subclasses must implement this method to return the login session type, which is usually
	 * a short identifier describing the target system, like 'ssc' or 'fod'. 
	 * @return login session type
	 */
	protected abstract String getSessionType();
	
	/**
	 * Subclasses must implement this method to return the login session data class
	 * @return login session data class
	 */
	protected abstract Class<D> getSessionDataClass();
}
