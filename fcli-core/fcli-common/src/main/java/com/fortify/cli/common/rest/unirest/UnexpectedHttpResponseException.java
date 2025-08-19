/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.unirest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.util.StringHelper;

import kong.unirest.HttpRequestSummary;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import kong.unirest.UnirestParsingException;
import lombok.Getter;

@Reflectable // Required for calling methods like getMessage() in fcli actions
public final class UnexpectedHttpResponseException extends UnirestException {
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper yamlObjectMapper = createYamlObjectMapper();
    @Getter private final int status;

    public UnexpectedHttpResponseException(HttpResponse<?> failureResponse) {
        super(getMessage(failureResponse), getCause(failureResponse));
        this.status = failureResponse.getStatus();
    }

    public UnexpectedHttpResponseException(HttpResponse<?> failureResponse, HttpRequestSummary requestSummary) {
        super(getMessage(failureResponse, requestSummary), getCause(failureResponse));
        this.status = failureResponse.getStatus();
    }

    private static final String getMessage(HttpResponse<?> failureResponse, HttpRequestSummary requestSummary) {
        var httpMethod = requestSummary.getHttpMethod().name();
        var url = requestSummary.getUrl();
        return StringHelper.indent(String.format("\nRequest: %s %s: %s", httpMethod, url, getMessage(failureResponse)), "  ");
    }

    private static final String getMessage(HttpResponse<?> failureResponse) {
        var reason = getFailureReason(failureResponse);
        var body = failureResponse.getParsingError()
                .map(UnirestParsingException::getOriginalBody)
                .map(UnexpectedHttpResponseException::formatBody)
                .orElse(formatBody(failureResponse.getBody()));
        return String.format("\nReason: %s\nBody: %s", reason, body);
    }
    
    private static final String getFailureReason(HttpResponse<?> failureResponse) {
        if ( isHttpFailure(failureResponse) ) {
            return String.format("HTTP %d %s", failureResponse.getStatus(), failureResponse.getStatusText());
        } else if ( failureResponse.getParsingError().isPresent() ) {
            return "Error parsing response";
        } else {
            return "Unknown";
        }
    }
    
    private static final String formatBody(Object body) {
        if ( body==null || (body instanceof String && StringUtils.isBlank((String)body)) ) { 
            return "<No Data>"; 
        } else if ( body instanceof JsonNode ) {
            try {
				return StringHelper.indent("\n"
						+ yamlObjectMapper.writeValueAsString(new SplitLinesJsonNodeWalker().walk((JsonNode) body)),
						"  ");
            } catch ( Exception ignore ) {} 
        }
		return StringHelper.indent("\n" + StringUtils.abbreviate(body.toString().trim(), 255), "  ") + "\n----";
    }

    private static final Throwable getCause(HttpResponse<?> failureResponse) {
        if ( isHttpFailure(failureResponse) ) { return null; }
        return failureResponse.getParsingError()
            .map(ExceptionUtils::getRootCause)
            .orElse(null);
    }
    
    private static final boolean isHttpFailure(HttpResponse<?> failureResponse) {
        int httpStatus = failureResponse.getStatus();
        return httpStatus < 200 || httpStatus >= 300;
    }
    
    private static final ObjectMapper createYamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.SPLIT_LINES)
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }
    
    private static final class SplitLinesJsonNodeWalker extends JsonNodeDeepCopyWalker {
        @SuppressWarnings("deprecation") // TODO Use non-deprecated class?
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( node instanceof TextNode ) {
                return new TextNode(WordUtils.wrap(node.asText(), 80));
            } else {
                return super.copyValue(state, path, parent, node);
            }
        }
    }
}