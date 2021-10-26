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
package com.fortify.cli.common.auth;

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
public final class AuthSessionPersistenceHelper {
	@Getter @Inject private ObjectMapper objectMapper;

	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final void saveData(String authSessionType, String authSessionName, Object authSessionData) {
		String authSessionDataJson = objectMapper.writeValueAsString(authSessionData);
		FcliHomeHelper.saveSecuredFile(Paths.get("authSessions", authSessionType, authSessionName), authSessionDataJson);
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final <T> T getData(String authSessionType, String authSessionName, Class<T> returnType) {
		String authSessionDataJson = FcliHomeHelper.readSecuredFile(Paths.get("authSessions", authSessionType, authSessionName), false);
		return authSessionDataJson==null ? null : objectMapper.readValue(authSessionDataJson, returnType);
	}
	
	public final boolean exists(String authSessionType, String authSessionName) {
		return FcliHomeHelper.isReadable(Paths.get("authSessions", authSessionType, authSessionName));
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final List<String> list(String authSessionType) {
		return FcliHomeHelper.listFilesInDir(Paths.get("authSessions", authSessionType), false)
				.map(Path::getFileName)
				.map(Path::toString)
				.collect(Collectors.toList());
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final Map<String,List<String>> listByAuthSessionType() {
		return FcliHomeHelper.listFilesInDir(Paths.get("authSessions"), true)
				.collect(Collectors.groupingBy(
					p->p.getParent().getFileName().toString(),
					Collectors.mapping(p->p.getFileName().toString(), Collectors.toList())
				));
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final void destroy(String authSessionType, String authSessionName) {
		FcliHomeHelper.deleteFile(Paths.get("authSessions", authSessionType, authSessionName));
	}
}
