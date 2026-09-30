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
package com.fortify.cli.ssc._common.rest.ssc.transfer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.exception.AbstractFcliException;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.HttpHeader;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.ProgressMonitor;
import kong.unirest.RawResponse;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class SSCFileTransferHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SSCFileTransferHelper.class);
    private static final JacksonObjectMapper XMLMAPPER = new JacksonObjectMapper(new XmlMapper());

    @SneakyThrows
    public static final File download(UnirestInstance unirest, String endpoint, File downloadPath, SSCFileTransferTokenType tokenType, ISSCAddDownloadTokenFunction addTokenFunction, IProgressWriter progressWriter) {
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, tokenType); ) {
            try ( SSCProgressMonitor downloadMonitor = new SSCProgressMonitor(progressWriter, "Download") ) {
                return addTokenFunction.apply(tokenSupplier.get(), unirest.get(endpoint))
                    .downloadMonitor(downloadMonitor)
                    .asFile(downloadPath.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
                    .getBody();
            }
        }
    }
    
    @SneakyThrows
    public static final File download(UnirestInstance unirest, String endpoint, File downloadPath, ISSCAddDownloadTokenFunction addTokenFunction, IProgressWriter progressWriter) {
        return download(unirest, endpoint, downloadPath, SSCFileTransferTokenType.DOWNLOAD, addTokenFunction, progressWriter);
    }

    /**
     * Downloads to any {@link Path}, including zip filesystem entry paths used by remediations cache.
     * Writes the body only after a successful non-202 status.
     */
    public static final void download(UnirestInstance unirest, String endpoint, Path downloadPath,
            ISSCAddDownloadTokenFunction addTokenFunction, IProgressWriter progressWriter) {
        download(unirest, endpoint, downloadPath, SSCFileTransferTokenType.DOWNLOAD, addTokenFunction, progressWriter);
    }

    public static final void download(UnirestInstance unirest, String endpoint, Path downloadPath,
            SSCFileTransferTokenType tokenType, ISSCAddDownloadTokenFunction addTokenFunction, IProgressWriter progressWriter) {
        boolean completed = false;
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, tokenType);
                SSCProgressMonitor downloadMonitor = new SSCProgressMonitor(progressWriter, "Download") ) {
            HttpResponse<Integer> response = addTokenFunction.apply(tokenSupplier.get(), unirest.get(endpoint))
                    .downloadMonitor(downloadMonitor)
                    .asObject(raw -> copyBodyIfReady(raw, downloadPath));
            int status = response.getBody() != null ? response.getBody() : response.getStatus();
            if (status < 200 || status >= 300 || status == 202) {
                throw new FcliSimpleException("Download failed with HTTP status " + status + " for " + endpoint);
            }
            completed = true;
        } catch (AbstractFcliException e) {
            if (!completed) {
                deleteQuietly(downloadPath);
            }
            throw e;
        } catch (RuntimeException e) {
            if (!completed) {
                deleteQuietly(downloadPath);
            }
            throw new FcliTechnicalException("Error downloading " + endpoint + " to " + downloadPath, e);
        } catch (Exception e) {
            // AutoCloseable close can surface checked exceptions; preserve interrupt flag.
            if (!completed) {
                deleteQuietly(downloadPath);
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new FcliTechnicalException("Error downloading " + endpoint + " to " + downloadPath, e);
        }
    }

    /** Status first; write body only for ready 2xx (not 202). Drain otherwise so the connection can close. */
    private static int copyBodyIfReady(RawResponse raw, Path destination) {
        int status = raw.getStatus();
        try (InputStream in = raw.getContent()) {
            if (status < 200 || status >= 300 || status == 202) {
                in.transferTo(OutputStream.nullOutputStream());
                return status;
            }
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            return status;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error handling download response for " + destination, e);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warn("Failed to delete incomplete download: {}", path, e);
        }
    }

    @SneakyThrows
    public static final <T> T htmlUpload(UnirestInstance unirest, String endpoint, File filePath, ISSCAddUploadTokenFunction addTokenFunction, Class<T> returnType, IProgressWriter progressWriter) {
        if ( !isHtmlEndpoint(endpoint) ) {
            throw new FcliBugException("Uploads to %s should be done through SSCFileTransferHelper::restUpload", endpoint);
        }
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, SSCFileTransferTokenType.UPLOAD); ) {
            try ( SSCProgressMonitor uploadMonitor = new SSCProgressMonitor(progressWriter, "Upload") ) {
                return addTokenFunction.apply(tokenSupplier.get(), unirest.post(endpoint))
                    .multiPartContent() // Force multipart request with correct Content-Type header
                    .field("file", filePath)
                    .uploadMonitor(uploadMonitor)
                    // Use headerReplace to replace rather than add the Accept header (avoid duplicates with defaults)
                    .headerReplace(HttpHeader.ACCEPT, "application/xml") 
                    .withObjectMapper(XMLMAPPER)
                    .asObject(returnType).getBody();
            }
        }
    }
    
    @SneakyThrows
    public static final <T> T restUpload(UnirestInstance unirest, String endpoint, File filePath, Class<T> returnType, IProgressWriter progressWriter) {
        if ( isHtmlEndpoint(endpoint) ) {
            throw new FcliBugException("Uploads to %s should be done through SSCFileTransferHelper::htmlUpload", endpoint);
        }
        try ( SSCProgressMonitor uploadMonitor = new SSCProgressMonitor(progressWriter, "Upload") ) {
            return unirest.post(endpoint)
                    .multiPartContent() // Force multipart request with correct Content-Type header
                    .field("file", filePath)
                    .uploadMonitor(uploadMonitor)
                    .asObject(returnType).getBody();
        }
    }

    private static final boolean isHtmlEndpoint(String endpoint) {
        return endpoint.startsWith("/upload");
    }
    
    @FunctionalInterface
    public static interface ISSCAddFileTransferTokenFunction<T extends HttpRequest<?>> extends BiFunction<String, T, T> {}
    
    @FunctionalInterface
    public static interface ISSCAddDownloadTokenFunction extends ISSCAddFileTransferTokenFunction<GetRequest> {
        public static final ISSCAddDownloadTokenFunction ROUTEPARAM_DOWNLOADTOKEN = 
                (token, unirest) -> unirest.routeParam("downloadToken", token);
        public static final ISSCAddDownloadTokenFunction QUERYSTRING_MAT = 
                (token, unirest) -> unirest.queryString("mat", token);
        public static final ISSCAddDownloadTokenFunction AUTHHEADER = 
                (token, unirest) -> unirest.headerReplace(HttpHeader.AUTHORIZATION, "FortifyToken "+token);
    }
    
    @FunctionalInterface
    public static interface ISSCAddUploadTokenFunction extends ISSCAddFileTransferTokenFunction<HttpRequestWithBody> {
        public static final ISSCAddUploadTokenFunction ROUTEPARAM_UPLOADTOKEN = 
                (token, unirest) -> unirest.routeParam("uploadToken", token);
        public static final ISSCAddUploadTokenFunction QUERYSTRING_MAT = 
                (token, unirest) -> unirest.queryString("mat", token);
        public static final ISSCAddUploadTokenFunction AUTHHEADER = 
                (token, unirest) -> unirest.headerReplace(HttpHeader.AUTHORIZATION, "FortifyToken "+token);
    }
    
    @RequiredArgsConstructor
    private static final class SSCProgressMonitor implements ProgressMonitor, AutoCloseable {
        private final IProgressWriter progressWriter;
        private final String action;
        
        @Override
        public void accept(String field, String fileName, Long bytesWritten, Long totalBytes) {
            progressWriter.writeProgress(String.format("\r%s %s: %d of %d bytes complete", action, fileName, bytesWritten, totalBytes));
        }
        public void close() {
            progressWriter.clearProgress();
        }
    }
    
    public static enum SSCFileTransferTokenType {
        UPLOAD,
        DOWNLOAD,
        REPORT_FILE
    }
    
    public static final class SSCFileTransferTokenSupplier implements AutoCloseable, Supplier<String> {
        private final UnirestInstance unirest;
        private final String token;
        
        public SSCFileTransferTokenSupplier(UnirestInstance unirest, SSCFileTransferTokenType tokenType) {
            this.unirest = unirest;
            ObjectNode response = unirest.post("/api/v1/fileTokens")
                    .body(String.format("{ \"fileTokenType\": \"%s\"}", tokenType.name()))
                    // Use headerReplace to replace rather than add headers (avoid duplicates with defaults)
                    .headerReplace(HttpHeader.ACCEPT, "application/json")
                    .headerReplace(HttpHeader.CONTENT_TYPE, "application/json")
                    .asObject(ObjectNode.class)
                    .getBody();
            this.token = JsonHelper.evaluateSpelExpression(response, "data.token", String.class);
        }
        
        @Override
        public String get() {
            return token;
        }
        
        @Override
        public void close() {
            try {
                unirest.delete("/api/v1/fileTokens").getBody();
            } catch (Exception e) {
                // TODO Log warning
            }
        }
    }
}
