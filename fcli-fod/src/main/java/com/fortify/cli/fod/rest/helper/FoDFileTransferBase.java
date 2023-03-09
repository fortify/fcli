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

package com.fortify.cli.fod.rest.helper;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.ProgressHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.helper.FoDImportScanSessionDescriptor;
import com.fortify.cli.fod.util.FoDConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.ProgressMonitor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

@ReflectiveAccess
public abstract class FoDFileTransferBase {
    @Getter private static final ObjectMapper objectMapper = new ObjectMapper();
    @Getter UnirestInstance unirest;
    @Getter HttpRequest<?> endpoint;
    @Getter File uploadFile;
    @Getter @Setter int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;

    protected String importScanSessionId;
    private long fileLen;

    public FoDFileTransferBase(UnirestInstance unirest, HttpRequest<?> endpoint, File uploadFile) {
        this.unirest = unirest;
        this.endpoint = endpoint;
        this.uploadFile = uploadFile;
        if (!uploadFile.exists() || !uploadFile.canRead())
            throw new ValidationException("Could not read file: " + uploadFile.getPath());
        this.fileLen = uploadFile.length();
    }

    @SneakyThrows
    public FoDUploadResponse upload() {
        File f = this.uploadFile;
        long fileLen = f.length();

        FoDProgressMonitor progressMonitor = new FoDProgressMonitor("Upload");
        //System.out.println("Uploading file: " + f.getPath());

        try (FileInputStream fs = new FileInputStream(f)) {
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

                //System.out.println("Uploading " + (fragmentNumber == -1 ? "end fragment " : "fragment " + fragmentNumber));
                HttpResponse<String> request = getUnirest().request(
                                String.valueOf(endpoint.getHttpMethod()), getUri(fragmentNumber++, offset).toString()
                        )
                        .contentType("application/octet-stream")
                        .header("Accept", "application/json")
                        .body(sendByteArray)
                        .asString();

                progressMonitor.accept(endpoint.getUrl(), f.getName(), offset, fileLen);

                offset += byteCount;

                //System.out.println("Status: " + request.getStatusText());

                if (request.getStatus() != 202) {
                    // final response has 200, try to deserialize it
                    if (request.getStatus() == 200) {
                        FoDUploadResponse response = objectMapper.readValue(request.getBody(), FoDUploadResponse.class);
                        progressMonitor.progressHelper.clearProgress();
                        return response;
                    } else if (!request.isSuccess()) {
                        FoDErrorResponse errors = objectMapper.readValue(request.getBody(), FoDErrorResponse.class);
                        /*if (errors != null) {
                            if (errors.toString().contains("Can not start another import is in progress")) {
                                System.out.println(errors.toString());
                            } else {
                                System.out.println("Package upload failed for the following reasons: ");
                                System.out.println(errors.toString());
                            }
                        } else {
                            if (!StringUtils.isNotBlank(request.getBody())) System.out.println("Raw response\n" + request.getBody());
                            else System.out.println("No response body from API");
                        }*/
                        throw new RuntimeException("Error starting scan:"+ (errors != null ? errors.toString() : ""));
                    }
                } else {
                    // consume body
                    if (request.getBody() != null) {
                        request.getBody();
                    }
                }

            }

        } catch (Exception ex) {
            throw new RuntimeException("Error starting scan:" + ex.getLocalizedMessage());
        } finally {
            progressMonitor.close();
        }

        return null;
    }

    //

    protected URI getUri(int fragmentNumber, long offset) throws URISyntaxException {
        URI uri = URI.create(endpoint.getUrl());
        if (importScanSessionId != null) {
            uri = appendUri(uri, "importScanSessionId="+importScanSessionId);
            uri = appendUri(uri, "fileLength="+fileLen);
        }
        uri = appendUri(uri, "fragNo="+fragmentNumber);
        uri = appendUri(uri,"offset="+offset);
        return uri;
    }

    protected FoDImportScanSessionDescriptor getImportScanSessionDescriptor(String relId) {
        GetRequest request = getUnirest().get(FoDUrls.RELEASE_IMPORT_SCAN_SESSION)
                .routeParam("relId", relId);
        JsonNode session = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(session, FoDImportScanSessionDescriptor.class);
    }

    //

    @RequiredArgsConstructor
    private static final class FoDProgressMonitor implements ProgressMonitor, AutoCloseable {
        private final ProgressHelper.IProgressHelper progressHelper = ProgressHelper.createProgressHelper();
        private final String action;

        @Override
        public void accept(String field, String fileName, Long bytesWritten, Long totalBytes) {
            progressHelper.writeProgress(String.format("\r%s %s: %d of %d bytes complete", action, fileName, bytesWritten, totalBytes));
        }
        public void close() {
            progressHelper.clearProgress();
        }
    }

    private static URI appendUri(URI uri, String appendQuery) throws URISyntaxException {
        URI oldUri = uri;
        return new URI(oldUri.getScheme(), oldUri.getAuthority(), oldUri.getPath(),
                oldUri.getQuery() == null ? appendQuery : oldUri.getQuery() + "&" + appendQuery, oldUri.getFragment());
    }
}
