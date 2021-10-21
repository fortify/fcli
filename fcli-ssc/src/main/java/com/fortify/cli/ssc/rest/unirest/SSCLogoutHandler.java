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
package com.fortify.cli.ssc.rest.unirest;

import com.fortify.cli.session.ILogoutHandler;
import com.fortify.cli.session.LoginSessionHelper;
import com.fortify.cli.ssc.rest.data.SSCLoginSessionData;
import com.fortify.cli.ssc.rest.data.SSCTokenResponse;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.RequestBodyEntity;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class SSCLogoutHandler implements ILogoutHandler {
	@Getter @Inject private LoginSessionHelper loginSessionHelper;
	@Getter @Inject private SSCUnirestRunner unirestRunner;

	@Override
	public final void logout(String loginSessionName) {
		SSCLoginSessionData data = loginSessionHelper.getData(getLoginSessionType(), loginSessionName, SSCLoginSessionData.class);
		unirestRunner.runWithUnirest(loginSessionName, unirestInstance->logout(unirestInstance, data));
	}
	
	private final Void logout(UnirestInstance unirestInstance, SSCLoginSessionData loginSessionData) {
		SSCTokenResponse cachedTokenResponse = loginSessionData.getCachedTokenResponse();
		if ( cachedTokenResponse != null ) {
			String token = String.valueOf(cachedTokenResponse.getData().getToken());
			String body = "{\"tokens\": [\""+token+"\"]}";
			System.out.println(body);
			RequestBodyEntity request = unirestInstance.post("/api/v1/tokens/action/revoke").body(body).contentType("application/json");
			System.out.println(request.asString().getBody());
		}
		return null;
	}

	@Override
	public String getLoginSessionType() {
		return "ssc";
	}
}
