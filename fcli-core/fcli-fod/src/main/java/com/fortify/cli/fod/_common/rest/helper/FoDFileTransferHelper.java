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

package com.fortify.cli.fod._common.rest.helper;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.common.rest.unirest.URIHelper;
import com.fortify.cli.fod._common.util.FoDConstants;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.ProgressMonitor;
import kong.unirest.core.UnirestInstance;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

// TODO Based on some recent messages on FortifySSC, potentially we may need 
//      chunked uploads for SC DAST as well, so consider refactoring into a
//      generic class in fcli-common.
public final class FoDFileTransferHelper {
    private static final int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;

    @SneakyThrows
    public static final JsonNode uploadChunked(UnirestInstance unirest, HttpRequest<?> baseRequest, File f) {
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException("Could not read file: " + f.getPath());
        }
        long fileLen = f.length();

        String lastBody = null;
        try (var fs = new FileInputStream(f); var progressMonitor = new FoDProgressMonitor("Upload"); ) {
            byte[] readByteArray = new byte[chunkSize];
            byte[] sendByteArray;
            int fragmentNumber = 0;
            int byteCount;
            long offset = 0;

            // loop through chunks
            while ((byteCount = fs.read(readByteArray)) != -1) {

                if (byteCount < chunkSize) {
                    sendByteArray = Arrays.copyOf(readByteArray, byteCount);
                    fragmentNumber = -1;
                } else {
                    sendByteArray = readByteArray;
                }

                lastBody = unirest.request(
                                String.valueOf(baseRequest.getHttpMethod()), 
                                getUri(baseRequest, fragmentNumber++, offset))
                        .contentType("application/octet-stream")
                        .header("Accept", "application/json")
                        .body(sendByteArray)
                        .asString()
                        .getBody();
                progressMonitor.accept(baseRequest.getUrl(), f.getName(), offset, fileLen);
                offset += byteCount;
            }
            progressMonitor.accept(baseRequest.getUrl(), f.getName(), offset, fileLen);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file", e);
        }

        return new ObjectMapper().readTree(lastBody);
    }

    private static final String getUri(HttpRequest<?> baseRequest, int fragmentNumber, long offset) throws URISyntaxException {
        URI uri = URI.create(baseRequest.getUrl());
        uri = URIHelper.addOrReplaceParam(uri, "fragNo", fragmentNumber);
        uri = URIHelper.addOrReplaceParam(uri, "offset", offset);
        return uri.toString();
    }

    @RequiredArgsConstructor
    private static final class FoDProgressMonitor implements ProgressMonitor, AutoCloseable {
        private final IProgressWriter progressWriter = ProgressWriterType.auto.create();
        private final String action;

        @Override
        public void accept(String field, String fileName, Long bytesWritten, Long totalBytes) {
            progressWriter.writeProgress(String.format("\r%s %s: %d of %d bytes complete", action, fileName, bytesWritten, totalBytes));
        }
        public void close() {
            progressWriter.clearProgress();
        }
    }
}
