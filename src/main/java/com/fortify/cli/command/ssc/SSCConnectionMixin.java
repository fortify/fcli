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
package com.fortify.cli.command.ssc;

import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.Getter;
import picocli.CommandLine.Option;

public class SSCConnectionMixin {
	@Getter private final ObjectMapper objectMapper;
	
	@Option(names = {"--ssc-url"}, description = "SSC URL", required = true)
	@Getter private String sscUrl;
	
	@Option(names = {"--ssc-user"}, description = "SSC User", required = true)
	@Getter private String sscUser;
	
	@Option(names = {"--ssc-password"}, description = "SSC Password", required = true)
	@Getter private String sscPassword;
	
	@Inject
	public SSCConnectionMixin(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	public <T> T executeWithUnirest(Function<UnirestInstance, T> requestExecutor) {
		try (UnirestInstance unirest = Unirest.spawnInstance()) {
			unirest.config()
				.setObjectMapper(new JacksonObjectMapper(objectMapper))
				.defaultBaseUrl(sscUrl)
				.addDefaultHeader("Authorization", "FortifyToken "+getAuthToken(unirest));
			return requestExecutor.apply(unirest);
		}
	}

	private String getAuthToken(UnirestInstance unirest) {
		SSCTokenRequest tokenRequest = SSCTokenRequest.builder().type("UnifiedLoginToken").build();
		System.out.println("tokenRequest: "+tokenRequest);
		SSCTokenResponse tokenResponse = unirest.post("/api/v1/tokens")
			.accept("application/json")
			.header("Content-Type", "application/json")
			.basicAuth(sscUser, sscPassword)
			.body(tokenRequest)
			.asObject(SSCTokenResponse.class).getBody();
		System.out.println(tokenResponse);
		return tokenResponse.getData().getToken();
	}
}
