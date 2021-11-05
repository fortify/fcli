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
package com.fortify.cli.common.rest.unirest;

import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.rest.data.BasicConnectionConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// TODO For now this class instantiates a new UnirestInstance on every call to runWithUnirest,
//      which should be OK when running individual commands but less performant when running
//      multiple commands in a composite command or workflow.
@ReflectiveAccess @Singleton
public class BasicConnectionConfigUnirestRunner {
	@Getter @Inject private UnirestRunner unirestRunner;
	@Getter @Inject private ObjectMapper objectMapper;
	
	/**
	 * Run the given runner with a {@link UnirestInstance} that has been configured
	 * based on the given {@link BasicConnectionConfig}.
	 * @param <R> Return type
	 * @param basicConnectionConfig with which to configure the connection
	 * @param runner to perform the actual work with a configured {@link UnirestInstance}
	 * @return Return value of runner; note that this return value shouldn't contain any reference to the 
	 *         {@link UnirestInstance} as that might be closed once this call returns.
	 */
	public <R> R runWithUnirest(BasicConnectionConfig basicConnectionConfig, Function<UnirestInstance, R> runner) {
		if ( basicConnectionConfig == null ) {
			throw new IllegalStateException("Connection configuration data may not be null");
		}
		return unirestRunner.runWithUnirest(unirest -> {
			_configure(basicConnectionConfig, unirest);
			return runner.apply(unirest);
		});
	}

	/**
	 * Perform basic connection configuration.
	 * @param basicConnectionConfig used to configure the {@link UnirestInstance}
	 * @param unirestInstance {@link UnirestInstance} to be configured
	 */
	private final void _configure(BasicConnectionConfig basicConnectionConfig, UnirestInstance unirestInstance) {
		unirestInstance.config()
			.defaultBaseUrl(normalizeUrl(basicConnectionConfig.getUrl()))
			.verifySsl(basicConnectionConfig.isInsecureModeEnabled());
		if ( StringUtils.isNotEmpty(basicConnectionConfig.getProxyHost()) ) {
			unirestInstance.config().proxy(basicConnectionConfig.getProxyHost(), basicConnectionConfig.getProxyPort(), basicConnectionConfig.getProxyUser(), 
					basicConnectionConfig.getProxyHost()==null ? null : String.valueOf(basicConnectionConfig.getProxyPassword()));
		}
	}
	
	protected String normalizeUrl(String url) {
		// We remove any trailing slashes, assuming that most users will specify relative URL's starting with /
		return url.replaceAll("/+$", "");
	}
}
