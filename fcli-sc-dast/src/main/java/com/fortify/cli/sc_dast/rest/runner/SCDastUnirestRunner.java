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
package com.fortify.cli.sc_dast.rest.runner;

import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.rest.runner.BasicUnirestRunner;
import com.fortify.cli.common.rest.runner.ThrowUnexpectedHttpResponseExceptionInterceptor;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.rest.unirest.runner.SSCAuthenticatedUnirestRunner;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class SCDastUnirestRunner {
	@Getter @Inject private BasicUnirestRunner basicUnirestRunner;
	@Getter @Inject private SSCAuthenticatedUnirestRunner sscUnirestRunner;
	
	public <R> R runWithUnirest(String sscAuthSessionName, Function<UnirestInstance, R> runner) {
		return sscUnirestRunner.runWithUnirest(sscAuthSessionName, sscUnirest -> {
			String scDastApiUrl = getSCDastApiUrlFromSSC(sscUnirest);
			String authHeader = sscUnirest.config().getDefaultHeaders().get("Authorization").stream().filter(h->h.startsWith("FortifyToken")).findFirst().orElseThrow();
			return basicUnirestRunner.runWithUnirest(scDastUnirest->{
				scDastUnirest.config()
						.defaultBaseUrl(scDastApiUrl)
						.setDefaultHeader("Authorization", authHeader)
						.verifySsl(sscUnirest.config().isVerifySsl());
				ThrowUnexpectedHttpResponseExceptionInterceptor.configure(scDastUnirest);
				return runner.apply(scDastUnirest);
			});
		});
	}

	private String getSCDastApiUrlFromSSC(UnirestInstance sscUnirest) {
		ArrayNode properties = getSCDastConfigurationProperties(sscUnirest);
		checkSCDastIsEnabled(properties);
		String scDastUrl = getSCDastUrlFromProperties(properties);
		return normalizeSCDastUrl(scDastUrl);
	}

	private final ArrayNode getSCDastConfigurationProperties(UnirestInstance sscUnirest) {
		ObjectNode configData = sscUnirest.get("/api/v1/configuration?group=edast")
				.asObject(ObjectNode.class)
				.getBody(); 
		
		return JsonHelper.evaluateJsonPath(configData, "$.data.properties", ArrayNode.class);
	}
	
	private void checkSCDastIsEnabled(ArrayNode properties) {
		boolean scDastEnabled = JsonHelper.evaluateJsonPath(properties, "$.[?(@.name=='edast.enabled')].value", Boolean.class);
		if (!scDastEnabled) {
			throw new IllegalStateException("ScanCentral DAST must be enabled in SSC");
		}
	}
	
	private String getSCDastUrlFromProperties(ArrayNode properties) {
		String scDastUrl = JsonHelper.evaluateJsonPath(properties, "$.[?(@.name=='edast.server.url')].value", String.class);
		if ( scDastUrl.isEmpty() ) {
			throw new IllegalStateException("SSC returns an empty ScanCentral DAST URL");
		}
		return scDastUrl;
	}
	
	private String normalizeSCDastUrl(String scDastUrl) {
		// We remove '/api' and any trailing slashes from the URL as most users will specify relative URL's starting with /api/v2/...
		return scDastUrl.replaceAll("/api/?$","").replaceAll("/+$", "");
	}
}
