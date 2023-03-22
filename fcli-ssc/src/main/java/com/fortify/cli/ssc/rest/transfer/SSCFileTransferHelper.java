/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.rest.transfer;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressHelper;
import com.fortify.cli.common.progress.helper.ProgressHelperFactory;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.ObjectMapper;
import kong.unirest.ProgressMonitor;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class SSCFileTransferHelper {
    private static final JacksonObjectMapper XMLMAPPER = new JacksonObjectMapper(new XmlMapper());

    @SneakyThrows
    public static final File download(UnirestInstance unirest, String endpoint, String downloadPath, ISSCAddDownloadTokenFunction addTokenFunction) {
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, SSCFileTransferTokenType.DOWNLOAD); ) {
            try ( SSCProgressMonitor downloadMonitor = new SSCProgressMonitor("Download") ) {
                return addTokenFunction.apply(tokenSupplier.get(), unirest.get(endpoint))
                    .downloadMonitor(downloadMonitor)
                    .asFile(downloadPath, StandardCopyOption.REPLACE_EXISTING)
                    .getBody();
            }
        }
    }

    @SneakyThrows
    public static final <T> T upload(UnirestInstance unirest, String endpoint, String filePath, ISSCAddUploadTokenFunction addTokenFunction, Class<T> returnType) {
        try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, SSCFileTransferTokenType.UPLOAD); ) {
            File f = new File(filePath);
            
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
                    .field("file", f)
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
        private final IProgressHelper progressHelper = ProgressHelperFactory.createProgressHelper(false);
        private final String action;
        
        @Override
        public void accept(String field, String fileName, Long bytesWritten, Long totalBytes) {
            progressHelper.writeProgress(String.format("\r%s %s: %d of %d bytes complete", action, fileName, bytesWritten, totalBytes));
        }
        public void close() {
            progressHelper.clearProgress();
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
            this.token = JsonHelper.evaluateSpELExpression(response, "data.token", String.class);
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
