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
import java.time.Duration;
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

@Slf4j
public final class RestResponseBodyHelper {
    private static final String TEMP_FILE_PREFIX = ".fcli-";
    private static final String TEMP_FILE_SUFFIX = ".part";
    private static final int BINARY_SNIFF_LENGTH = 8192;
    private static final int HTTP_ACCEPTED = 202;
    private static final Set<String> TEXT_MEDIA_TYPES = Set.of(
            "application/xml", "application/json", "application/csv",
            "application/yaml", "application/x-yaml", "application/javascript");

    private RestResponseBodyHelper() {}

    public static RestResponseFileRecord saveToFile(HttpRequest<?> request, Path destination, ProgressMonitor monitor) {
        return saveToFile(request, getTarget(destination), monitor, false);
    }

    public static RestResponseFileRecord saveToFileWhenReady(HttpRequest<?> request, Path destination, ProgressMonitor monitor, Duration pollInterval, Duration initialDelay) {
        var target = getTarget(destination);
        sleep(initialDelay);
        while ( true ) {
            var result = saveToFile(request, target, monitor, true);
            if ( result!=null ) { return result; }
            sleep(pollInterval);
        }
    }

    private static RestResponseFileRecord saveToFile(HttpRequest<?> request, Path target, ProgressMonitor monitor, boolean skipAccepted) {
        Path temp = null;
        try {
            temp = Files.createTempFile(target.getParent(), TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            if ( monitor!=null ) {
                var fileName = target.getFileName().toString();
                request.downloadMonitor((field, ignored, bytesWritten, totalBytes) -> monitor.accept(field, fileName, bytesWritten, totalBytes));
            }
            var response = request.asFile(temp.toString(), StandardCopyOption.REPLACE_EXISTING);
            // Instances without the fcli unexpected response interceptor don't throw on error statuses
            if ( !response.isSuccess() ) { throw new UnexpectedHttpResponseException(response); }
            if ( skipAccepted && response.getStatus()==HTTP_ACCEPTED ) { return null; }
            moveIntoPlace(temp, target);
            return new RestResponseFileRecord(target, Files.size(target), getContentType(response), response.getStatus());
        } catch ( IOException e ) {
            throw new FcliTechnicalException("Error saving response to "+target, e);
        } finally {
            deleteQuietly(temp);
        }
    }

    private static Path getTarget(Path destination) {
        var target = destination.toAbsolutePath().normalize();
        var parent = target.getParent();
        if ( parent==null || !Files.isDirectory(parent) ) {
            throw new FcliSimpleException("Directory does not exist: "+parent);
        }
        return target;
    }

    private static void sleep(Duration duration) {
        if ( duration==null || duration.isZero() || duration.isNegative() ) { return; }
        try {
            Thread.sleep(duration.toMillis());
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new FcliTechnicalException("Interrupted while waiting for file to become available", e);
        }
    }

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
            }
        }
    }
}
