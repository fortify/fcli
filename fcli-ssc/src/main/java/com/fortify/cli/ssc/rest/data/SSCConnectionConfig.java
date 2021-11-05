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
package com.fortify.cli.ssc.rest.data;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fortify.cli.common.rest.data.BasicConnectionConfig;
import com.fortify.cli.common.rest.data.BasicUserCredentialsConfig;
import com.fortify.cli.common.rest.data.IBasicConnectionConfigProvider;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.util.StringUtils;
import lombok.Data;

@Introspected @Data @JsonIgnoreProperties(ignoreUnknown = true) 
public class SSCConnectionConfig implements IBasicConnectionConfigProvider {
	private BasicConnectionConfig basicConnectionConfig;
	private BasicUserCredentialsConfig basicUserCredentialsConfig;
	private OffsetDateTime expiresAt;
	private char[] token;
	
	public void setToken(char[] token) {
		this.token = token==null ? null : toBase64Token(token);
	}
	
	/**
	 * Make sure that we're using a Base64 encoded token as required by SSC
	 * @param token Encoded or non-encoded token
	 * @return Base64 encoded token
	 */
	private final char[] toBase64Token(char[] token) {
		final byte[] tokenBytes = toByteArray(token);
		final byte[] encodedToken = Base64.isBase64(tokenBytes) ? tokenBytes : Base64.encodeBase64(tokenBytes);
		return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encodedToken)).array();
	}
	
	private final byte[] toByteArray(char[] input) {
		ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(input));
		byte[] result = new byte[bb.remaining()];
		bb.get(result);
		return result;
	}

	@JsonIgnore public final boolean hasUserCredentialsConfig() {
		return basicUserCredentialsConfig!=null 
				&& StringUtils.isNotEmpty(basicUserCredentialsConfig.getUser())
				&& basicUserCredentialsConfig.getPassword()!=null;
	}
	
	@JsonIgnore public final BasicConnectionConfig getNonNullBasicConnectionConfig() {
		basicConnectionConfig = basicConnectionConfig==null ? new BasicConnectionConfig() : basicConnectionConfig;
		return basicConnectionConfig;
	}
	
	@JsonIgnore public final BasicUserCredentialsConfig getNonNullBasicUserCredentialsConfig() {
		basicUserCredentialsConfig = basicUserCredentialsConfig==null ? new BasicUserCredentialsConfig() : basicUserCredentialsConfig;
		return basicUserCredentialsConfig;
	}
}
