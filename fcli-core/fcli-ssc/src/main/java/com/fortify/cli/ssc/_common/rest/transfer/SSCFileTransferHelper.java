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
package com.fortify.cli.ssc._common.rest.transfer;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import kong.unirest.core.GetRequest;
import kong.unirest.core.HttpRequest;
import kong.unirest.core.HttpRequestWithBody;
import kong.unirest.core.ObjectMapper;
import kong.unirest.core.ProgressMonitor;
import kong.unirest.core.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class SSCFileTransferHelper {
    private static final JacksonObjectMapper XMLMAPPER = new JacksonObjectMapper(new XmlMapper());

    @SneakyThrows
    public static final File download(UnirestInstance unirest, String endpoint, File downloadPath, ISSCAddDownloadTokenFunction addTokenFunction) {
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, SSCFileTransferTokenType.DOWNLOAD); ) {
            try ( SSCProgressMonitor downloadMonitor = new SSCProgressMonitor("Download") ) {
                return addTokenFunction.apply(tokenSupplier.get(), unirest.get(endpoint))
                    .downloadMonitor(downloadMonitor)
                    .asFile(downloadPath.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
                    .getBody();
            }
        }
    }

    @SneakyThrows
    public static final <T> T upload(UnirestInstance unirest, String endpoint, File filePath, ISSCAddUploadTokenFunction addTokenFunction, Class<T> returnType) {
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, SSCFileTransferTokenType.UPLOAD); ) {
            String acceptHeaderValue = unirest.config().getDefaultHeaders().getFirst("Accept");
            ObjectMapper objectMapper = unirest.config().getObjectMapper();
            if ( endpoint.startsWith("/upload") ) {
                // SSC always returns XML data on these endpoints, so we use the appropriate Accept header and ObjectMapper
                acceptHeaderValue = "application/xml";
                objectMapper = XMLMAPPER;
            }
            
            try ( SSCProgressMonitor uploadMonitor = new SSCProgressMonitor("Upload") ) {
                return addTokenFunction.apply(tokenSupplier.get(), unirest.post(endpoint))
                    .multiPartContent() // Force multipart request with correct Content-Type header
                    .field("file", filePath)
                    .uploadMonitor(uploadMonitor)
                    .headerReplace("Accept", acceptHeaderValue) 
                    .withObjectMapper(objectMapper)
                    .asObject(returnType).getBody();
            }
        }
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
                (token, unirest) -> unirest.headerReplace("Authorization", "FortifyToken "+token);
    }
    
    @FunctionalInterface
    public static interface ISSCAddUploadTokenFunction extends ISSCAddFileTransferTokenFunction<HttpRequestWithBody> {
        public static final ISSCAddUploadTokenFunction ROUTEPARAM_UPLOADTOKEN = 
                (token, unirest) -> unirest.routeParam("uploadToken", token);
        public static final ISSCAddUploadTokenFunction QUERYSTRING_MAT = 
                (token, unirest) -> unirest.queryString("mat", token);
        public static final ISSCAddUploadTokenFunction AUTHHEADER = 
                (token, unirest) -> unirest.headerReplace("Authorization", "FortifyToken "+token);
    }
    
    @RequiredArgsConstructor
    private static final class SSCProgressMonitor implements ProgressMonitor, AutoCloseable {
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
    
    private static enum SSCFileTransferTokenType {
        UPLOAD,
        DOWNLOAD
    }
    
    private static final class SSCFileTransferTokenSupplier implements AutoCloseable, Supplier<String> {
        private final UnirestInstance unirest;
        private final String token;
        
        public SSCFileTransferTokenSupplier(UnirestInstance unirest, SSCFileTransferTokenType tokenType) {
            this.unirest = unirest;
            ObjectNode response = unirest.post("/api/v1/fileTokens")
                    .body(String.format("{ \"fileTokenType\": \"%s\"}", tokenType.name()))
                    .accept("application/json")
                    .contentType("application/json")
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
