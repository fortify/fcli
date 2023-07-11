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
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.fod.entity.scan.helper.FoDImportScanSessionDescriptor;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.util.FoDConstants;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.ProgressMonitor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

// TODO Based on some recent messages on FortifySSC, potentially we may need 
//      chunked uploads for SC DAST as well, so consider refactoring into a
//      generic class in fcli-common.
// TODO Why is this an abstract class? It doesn't define any abstract methods
//      that need to be overridden in subclasses for example. Consider refactoring
//      this into a regular utility class.
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
                        progressMonitor.progressWriter.clearProgress();
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

    private static URI appendUri(URI uri, String appendQuery) throws URISyntaxException {
        URI oldUri = uri;
        return new URI(oldUri.getScheme(), oldUri.getAuthority(), oldUri.getPath(),
                oldUri.getQuery() == null ? appendQuery : oldUri.getQuery() + "&" + appendQuery, oldUri.getFragment());
    }
}
