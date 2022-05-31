package com.fortify.cli.fod.util;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;

import io.micronaut.http.uri.UriBuilder;
import kong.unirest.HttpResponse;

public class FoDOutputHelper {
	public static final OutputConfig defaultTableOutputConfig() {
		return OutputConfig.table().inputTransformer(json -> json.get("items"));
	}

	public static final Function<HttpResponse<JsonNode>, String> pagingHandler(final String uri) {
		return r -> {
			JsonNode body = r.getBody();
			int offset = body.get("offset").asInt();
			int totalCount = body.get("totalCount").asInt();
			int limit = body.get("limit").asInt();
			int newOffset = offset + limit;
			if (newOffset < totalCount) {
				return UriBuilder.of(uri).replaceQueryParam("offset", newOffset).build().toString();
			}
			return null;
		};
	}
}
