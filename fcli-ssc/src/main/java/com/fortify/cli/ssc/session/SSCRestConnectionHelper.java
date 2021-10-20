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
package com.fortify.cli.ssc.session;

import java.util.Date;

import com.fortify.cli.rest.connection.AbstractRestConnectionHelper;
import com.fortify.cli.rest.connection.UnirestInstanceFactory;
import com.fortify.cli.session.LoginSessionHelper;
import com.fortify.cli.ssc.rest.connection.SSCTokenResponse.SSCTokenData;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class SSCRestConnectionHelper extends AbstractRestConnectionHelper<SSCLoginSessionData> {
	@Inject public SSCRestConnectionHelper(UnirestInstanceFactory unirestInstanceFactory, LoginSessionHelper loginSessionHelper) {
		super(unirestInstanceFactory, loginSessionHelper);
	}

	@Override
	public final String getLoginSessionType() {
		return "ssc";
	}

	@Override
	protected Class<SSCLoginSessionData> getSessionDataClass() {
		return SSCLoginSessionData.class;
	}
	
	@Override
	public void logout(SSCLoginSessionData sessionData) {
		var cachedTokenResponse = sessionData.getCachedTokenResponse();
		var tokenData = cachedTokenResponse==null ? null : cachedTokenResponse.getData();
		if ( tokenData!=null ) {
			logout(tokenData);
		}
	}

	private void logout(SSCTokenData tokenData) {
		if ( tokenData.getTerminalDate().after(new Date())) {
			System.out.println("Logging out from SSC");
		}
	}
}
