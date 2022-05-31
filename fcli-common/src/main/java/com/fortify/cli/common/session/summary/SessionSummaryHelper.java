package com.fortify.cli.common.session.summary;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.session.ISessionTypeProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;

@Singleton
public class SessionSummaryHelper {
	@Inject private ObjectMapper objectMapper;
	@Getter private Map<String, ISessionSummaryProvider> sessionSummaryProviders;
	
	@Inject
	public SessionSummaryHelper(Collection<ISessionSummaryProvider> sessionSummaryProviders) {
		this.sessionSummaryProviders = sessionSummaryProviders.stream().collect(
			Collectors.toMap(ISessionTypeProvider::getSessionType, Function.identity()));
	}
	
	public final ISessionSummaryProvider getSessionSummaryProvider(String sessionType) {
		return sessionSummaryProviders.get(sessionType);
	}
	
	public final Collection<SessionSummary> getSessionSummaries(String sessionType) {
		return getSessionSummaryProvider(sessionType).getSessionSummaries();
	}
	
	public final void writeSessionSummaries(String sessionType, OutputMixin outputMixin) {
		try ( var writer = outputMixin.getWriter() ) {
			getSessionSummaries(sessionType).stream()
				.map(objectMapper::valueToTree)
				.map(JsonNode.class::cast) // TODO Not sure why this is necessary
				.forEach(writer::write);
		}
	}
}
