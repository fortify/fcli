/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.rest.unirest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.ProgressMonitor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper methods for handling REST response bodies that are not (necessarily) JSON,
 * shared by the rest call commands, the action rest.call step and the fcli download
 * commands. Error responses are never written to a destination file: bodies are first
 * written to a temporary file next to the destination, which is only moved into place
 * after a successful response.
 */
@Slf4j
public final class RestResponseBodyHelper {
    private static final String TEMP_FILE_PREFIX = ".fcli-";
    private static final String TEMP_FILE_SUFFIX = ".part";
    private static final int BINARY_SNIFF_LENGTH = 8192;
    private static final Set<String> TEXT_MEDIA_TYPES = Set.of(
            "application/xml", "application/json", "application/csv",
            "application/yaml", "application/x-yaml", "application/javascript");

    private RestResponseBodyHelper() {}

    /**
     * Save the body of a successful response for the given request to the given destination.
     * The destination is only replaced if the response is successful; on any failure, the
     * destination is left untouched and no temporary file remains.
     *
     * @param request request to execute
     * @param destination file to write; its parent directory must exist
     * @param monitor optional download progress monitor, may be null
     * @return description of the saved file
     * @throws FcliSimpleException if the parent directory of the destination does not exist
     * @throws UnexpectedHttpResponseException if the response status is not 2xx
     * @throws FcliTechnicalException on I/O errors while moving the downloaded file into place
     */
    public static RestResponseFileRecord saveToFile(HttpRequest<?> request, Path destination, ProgressMonitor monitor) {
        var target = destination.toAbsolutePath().normalize();
        var parent = target.getParent();
        if ( parent==null || !Files.isDirectory(parent) ) {
            throw new FcliSimpleException("Directory does not exist: "+parent);
        }
        Path temp = null;
        try {
            temp = Files.createTempFile(parent, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            if ( monitor!=null ) { request.downloadMonitor(monitor); }
            var response = request.asFile(temp.toString(), StandardCopyOption.REPLACE_EXISTING);
            // Instances without the fcli unexpected response interceptor don't throw on error statuses
            if ( !response.isSuccess() ) { throw new UnexpectedHttpResponseException(response); }
            moveIntoPlace(temp, target);
            return new RestResponseFileRecord(target, Files.size(target), getContentType(response), response.getStatus());
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Error saving response to "+target, e);
        } finally {
            deleteQuietly(temp);
        }
    }

    /**
     * Execute the given request and return the successful response body as text, decoded using
     * the charset declared in the Content-Type header, or UTF-8 if none is declared.
     *
     * @param request request to execute
     * @param binaryGuidance guidance appended to the error message if the response is binary,
     *        for example which option to use for saving binary content
     * @return response body as text
     * @throws FcliSimpleException if the response body is binary
     * @throws UnexpectedHttpResponseException if the response status is not 2xx
     */
    public static String asText(HttpRequest<?> request, String binaryGuidance) {
        var response = request.asBytes();
        if ( !response.isSuccess() ) { throw new UnexpectedHttpResponseException(response); }
        var contentType = getContentType(response);
        var body = response.getBody()==null ? new byte[0] : response.getBody();
        if ( !isText(contentType, body) ) {
            throw new FcliSimpleException(String.format("Response is binary (content type: %s); %s",
                    contentType==null ? "unknown" : contentType, binaryGuidance));
        }
        return new String(body, charsetOf(contentType));
    }

    /**
     * Execute the given request and parse the successful response body as JSON, exactly like
     * {@code asObject(JsonNode.class)} (same object mapper and charset handling; no content
     * results in null). If the body is not valid JSON, it is returned as a {@link TextNode} if
     * it is textual, unless the response declares a JSON content type.
     *
     * @param request request to execute
     * @param binaryGuidance guidance appended to the error message if the response is binary
     * @return parsed JSON, text node for textual non-JSON bodies, or null if there is no content
     * @throws FcliTechnicalException if a response declared as JSON cannot be parsed
     * @throws FcliSimpleException if the response is neither JSON nor text
     * @throws UnexpectedHttpResponseException if the response status is not 2xx
     */
    public static JsonNode asJsonOrText(HttpRequest<?> request, String binaryGuidance) {
        var response = request.asBytes();
        if ( !response.isSuccess() ) { throw new UnexpectedHttpResponseException(response); }
        var body = response.getBody();
        if ( body==null || body.length==0 ) { return null; }
        var contentType = getContentType(response);
        try {
            return JsonHelper.getObjectMapper().readTree(new String(body, charsetOf(contentType)));
        } catch ( JsonProcessingException e ) {
            if ( isJsonMediaType(getMediaType(contentType)) ) {
                throw new FcliTechnicalException("Response declared as JSON could not be parsed", e);
            } else if ( !isText(contentType, body) ) {
                throw new FcliSimpleException(String.format("Response is binary (content type: %s); %s",
                        contentType==null ? "unknown" : contentType, binaryGuidance));
            }
            log.debug("Response is not JSON; using text (content type: {})", contentType);
            return new TextNode(new String(body, charsetOf(contentType)));
        }
    }

    /**
     * Determine whether the given body should be treated as text. Bodies with a textual media type
     * are always considered text; for other media types (or no media type), the body is considered
     * text only if its first bytes contain no control characters other than those commonly found in
     * text (tab, line feed, form feed, carriage return, backspace, escape), and the whole body can be
     * decoded without errors using the declared charset (UTF-8 by default).
     */
    static boolean isText(String contentType, byte[] body) {
        var mediaType = getMediaType(contentType);
        if ( mediaType!=null && (mediaType.startsWith("text/") || TEXT_MEDIA_TYPES.contains(mediaType)
                || mediaType.endsWith("+xml") || mediaType.endsWith("+json")) ) {
            return true;
        }
        for ( int i = 0; i < Math.min(body.length, BINARY_SNIFF_LENGTH); i++ ) {
            if ( isBinaryControlCharacter(body[i]) ) { return false; }
        }
        try {
            charsetOf(contentType).newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(body));
            return true;
        } catch ( CharacterCodingException e ) {
            return false;
        }
    }

    /**
     * @return charset declared in the given Content-Type header value, or UTF-8 if no (valid)
     *         charset is declared
     */
    static Charset charsetOf(String contentType) {
        if ( contentType!=null ) {
            for ( var parameter : contentType.split(";") ) {
                var nameAndValue = parameter.trim().split("=", 2);
                if ( nameAndValue.length==2 && "charset".equalsIgnoreCase(nameAndValue[0].trim()) ) {
                    try {
                        return Charset.forName(StringUtils.strip(nameAndValue[1].trim(), "\""));
                    } catch ( IllegalArgumentException e ) {
                        return StandardCharsets.UTF_8;
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static boolean isBinaryControlCharacter(byte b) {
        return b>=0 && b<0x20 && b!='\t' && b!='\n' && b!='\f' && b!='\r' && b!='\b' && b!=0x1b;
    }

    private static boolean isJsonMediaType(String mediaType) {
        return mediaType!=null && (mediaType.equals("application/json") || mediaType.endsWith("+json"));
    }

    private static String getMediaType(String contentType) {
        return contentType==null ? null : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String getContentType(HttpResponse<?> response) {
        return StringUtils.trimToNull(response.getHeaders().getFirst(HttpHeader.CONTENT_TYPE));
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch ( AtomicMoveNotSupportedException e ) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        if ( path!=null ) {
            try {
                Files.deleteIfExists(path);
            } catch ( IOException ignore ) {
                // Best effort; a leftover temporary file is not worth failing the operation for
            }
        }
    }
}
