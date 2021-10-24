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
package com.fortify.cli.common.session;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.util.FcliHomeHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.SneakyThrows;

@Singleton @ReflectiveAccess
public final class LoginSessionHelper {
	@Getter @Inject private ObjectMapper objectMapper;

	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final void saveData(String loginSessionType, String loginSessionName, Object loginSessionData) {
		String loginSessionDataJson = objectMapper.writeValueAsString(loginSessionData);
		FcliHomeHelper.saveSecuredFile(Paths.get("loginSessions", loginSessionType, loginSessionName), loginSessionDataJson);
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final <T> T getData(String loginSessionType, String loginSessionName, Class<T> returnType) {
		String loginSessionDataJson = FcliHomeHelper.readSecuredFile(Paths.get("loginSessions", loginSessionType, loginSessionName), false);
		return loginSessionDataJson==null ? null : objectMapper.readValue(loginSessionDataJson, returnType);
	}
	
	public final boolean exists(String loginSessionType, String loginSessionName) {
		return FcliHomeHelper.isReadable(Paths.get("loginSessions", loginSessionType, loginSessionName));
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final List<String> list(String loginSessionType) {
		return FcliHomeHelper.listFilesInDir(Paths.get("loginSessions", loginSessionType), false)
				.map(Path::getFileName)
				.map(Path::toString)
				.collect(Collectors.toList());
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final Map<String,List<String>> listByLoginSessionType() {
		return FcliHomeHelper.listFilesInDir(Paths.get("loginSessions"), true)
				.collect(Collectors.groupingBy(
					p->p.getParent().getFileName().toString(),
					Collectors.mapping(p->p.getFileName().toString(), Collectors.toList())
				));
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final void destroy(String loginSessionType, String loginSessionName) {
		FcliHomeHelper.deleteFile(Paths.get("loginSessions", loginSessionType, loginSessionName));
	}
}
