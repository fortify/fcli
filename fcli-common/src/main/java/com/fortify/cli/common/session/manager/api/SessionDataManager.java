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
package com.fortify.cli.common.session.manager.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.home.FcliHomeHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.SneakyThrows;

@Singleton @ReflectiveAccess
public final class SessionDataManager {
	@Getter @Inject private ObjectMapper objectMapper;

	@SneakyThrows // TODO Do we want to use SneakyThrows? 
	public final void saveData(String authSessionType, String authSessionName, ISessionData sessionData) {
		String authSessionDataJson = objectMapper.writeValueAsString(sessionData);
		FcliHomeHelper.saveSecuredFile(Paths.get("authSessions", authSessionType, authSessionName), authSessionDataJson);
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final <T extends ISessionData> T getData(String authSessionType, String authSessionName, Class<T> returnType) {
		Path authSessionDataPath = Paths.get("authSessions", authSessionType, authSessionName);
		try {
			String authSessionDataJson = FcliHomeHelper.readSecuredFile(authSessionDataPath, false);
			return authSessionDataJson==null ? null : objectMapper.readValue(authSessionDataJson, returnType);
		} catch ( Exception e ) {
			FcliHomeHelper.deleteFile(authSessionDataPath);
			throw new IllegalStateException("Error reading auth session data, please try logging in again", e);
		}
	}
	
	public final boolean exists(String authSessionType, String authSessionName) {
		return FcliHomeHelper.isReadable(Paths.get("authSessions", authSessionType, authSessionName));
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final List<String> list(String authSessionType) {
		Path path = Paths.get("authSessions", authSessionType);
		if ( !FcliHomeHelper.exists(path) ) {
			return Collections.emptyList();
		}
		return FcliHomeHelper.listFilesInDir(path, false)
				.map(Path::getFileName)
				.map(Path::toString)
				.collect(Collectors.toList());
	}
	
	@SneakyThrows // TODO Do we want to use SneakyThrows?
	public final void destroy(String authSessionType, String authSessionName) {
		FcliHomeHelper.deleteFile(Paths.get("authSessions", authSessionType, authSessionName));
	}
}
