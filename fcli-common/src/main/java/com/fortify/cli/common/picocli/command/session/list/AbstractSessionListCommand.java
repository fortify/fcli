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
package com.fortify.cli.common.picocli.command.session.list;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.picocli.annotation.FixSuperclassInjection;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.session.ISessionTypeProvider;
import com.fortify.cli.common.session.summary.ISessionSummaryProvider;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = "sessions", description = "Get information related to authentication sessions.")
@FixSuperclassInjection
public abstract class AbstractSessionListCommand implements Runnable, ISessionTypeProvider {
	@Inject private ObjectMapper objectMapper;
	@Getter private Map<String, ISessionSummaryProvider> sessionSummaryProviders;
	@Mixin private OutputMixin outputMixin;
	
	@Inject
	public void setSessionSummaryProviders(Collection<ISessionSummaryProvider> sessionSummaryProviders) {
		this.sessionSummaryProviders = sessionSummaryProviders.stream().collect(
			Collectors.toMap(ISessionTypeProvider::getSessionType, Function.identity()));
	}

	@Override
	public void run() {
		try ( var writer = outputMixin.getWriter() ) {
			sessionSummaryProviders.get(getSessionType()).getSessionSummaries().stream()
				.map(objectMapper::valueToTree)
				.map(JsonNode.class::cast) // TODO Not sure why this is necessary
				.forEach(writer::write);
		}
	}
}
