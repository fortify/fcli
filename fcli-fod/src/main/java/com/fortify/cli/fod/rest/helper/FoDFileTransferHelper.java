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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.helper.FoDImportScanResponse;
import com.fortify.cli.fod.scan.helper.FoDImportScanSessionDescriptor;
import com.fortify.cli.fod.scan.helper.FoDStartScanResponse;
import com.fortify.cli.fod.util.FoDConstants;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.ProgressMonitor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.validation.ValidationException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

@ReflectiveAccess
public class FoDFileTransferHelper {
    @Getter private static final ObjectMapper objectMapper = new ObjectMapper();
    @Getter UnirestInstance unirest;
    @Getter @Setter int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;
    @Getter @Setter int uploadSyncTime = FoDConstants.DEFAULT_UPLOAD_SYNC_TIME;

    public FoDFileTransferHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    @SneakyThrows
    public FoDImportScanResponse importScan(String relId, String endpoint, String uploadFile) {

        File f = new File(uploadFile);
        if (!f.exists() || !f.canRead())
            throw new ValidationException("Could not read file: " + f.getPath());
        long fileLen = f.length();
        System.out.println("Uploading scan file: " + f.getPath());

        String importScanSessionId = getImportScanSessionDescriptor(relId).getImportScanSessionId();
        System.out.println("Created import scan session id: " + importScanSessionId);

        try (FileInputStream fs = new FileInputStream(uploadFile)) {
            byte[] readByteArray = new byte[chunkSize];
            byte[] sendByteArray;
            int fragmentNumber = 0;
            int byteCount;
            long offset = 0;
            int numFragments = (int) Math.ceil(fileLen / chunkSize);
            if (fileLen % chunkSize > 0) numFragments++;

            System.out.println("Total File Size = " + fileLen + " bytes");
            System.out.println("Upload Fragment Size = " + chunkSize + " bytes");
            System.out.println("Number of Fragments = " + numFragments);

            // loop through chunks
            while ((byteCount = fs.read(readByteArray)) != -1) {

                if (byteCount < chunkSize) {
                    sendByteArray = Arrays.copyOf(readByteArray, byteCount);
                    fragmentNumber = -1;
                } else {
                    sendByteArray = readByteArray;
                }

                //System.out.println("Uploading " + (fragmentNumber == -1 ? "end fragment " : "fragment " + fragmentNumber));
                HttpResponse<String> request = getUnirest().put(endpoint)
                        .routeParam("relId", relId)
                        .queryString("fragNo", fragmentNumber++)
                        .queryString("offset", offset)
                        .queryString("fileLength", fileLen)
                        .queryString("importScanSessionId", importScanSessionId)
                        .contentType("application/octet-stream")
                        .header("Accept", "application/json")
                        .body(sendByteArray)
                        //.uploadMonitor(new FoDProgressMonitor("Import", f.length()))
                        .asString();

                System.out.print(".");

                offset += byteCount;

                //System.out.println("Status: " + request.getStatusText());
                // introduce a minor delay otherwise upload stream can get out of sync! :(
                try {
                    Thread.sleep(uploadSyncTime * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                if (request.getStatus() != 202) {
                    // final response has 200, try to deserialize it
                    if (request.getStatus() == 200) {
                        FoDImportScanResponse response = objectMapper.readValue(request.getBody(), FoDImportScanResponse.class);
                        System.out.println("\nImport " + response.getReferenceId() + " uploaded successfully. Total bytes sent: "
                                + offset + " (Response: " + request.getStatusText() + ")");
                        return response;
                    } else if (!request.isSuccess()) {
                        System.out.println("An error occurred during the upload.");
                        FoDErrorResponse errors = objectMapper.readValue(request.getBody(), FoDErrorResponse.class);
                        if (errors != null) {
                            if (errors.toString().contains("Can not start another import is in progress")) {
                                System.out.println(errors);
                            } else {
                                System.out.println("Package upload failed for the following reasons: ");
                                System.out.println(errors);
                            }
                        } else {
                            if (!StringUtils.isNotBlank(request.getBody())) System.out.println("Raw response\n" + request.getBody());
                            else System.out.println("No response body from API");
                        }
                        throw new RuntimeException("Error importing scan:" + (errors != null ? errors.toString() : ""));
                    }
                }

            }

        } catch (Exception ex) {
            if (ex.getMessage().contains("More recent Scan exists")) {
                System.out.println("You cannot upload as a more recent scan already exists.");
            } else
                throw new RuntimeException("Error importing scan:" + ex.getLocalizedMessage());
        }

        return null;

    }

    @SneakyThrows
    public FoDStartScanResponse startScan(String endpoint, String uploadFile) {

        File f = new File(uploadFile);
        if (!f.exists() || !f.canRead())
            throw new ValidationException("Could not read file: " + f.getPath());
        long fileLen = f.length();
        System.out.println("Uploading scan file: " + f.getPath());

        try (FileInputStream fs = new FileInputStream(uploadFile)) {
            byte[] readByteArray = new byte[chunkSize];
            byte[] sendByteArray;
            int fragmentNumber = 0;
            int byteCount;
            long offset = 0;
            int numFragments = (int) Math.ceil(fileLen / chunkSize);
            if (fileLen % chunkSize > 0) numFragments++;

            System.out.println("Total File Size = " + fileLen + " bytes");
            System.out.println("Upload Fragment Size = " + chunkSize + " bytes");
            System.out.println("Number of Fragments = " + numFragments);

            // loop through chunks
            while ((byteCount = fs.read(readByteArray)) != -1) {

                if (byteCount < chunkSize) {
                    sendByteArray = Arrays.copyOf(readByteArray, byteCount);
                    fragmentNumber = -1;
                } else {
                    sendByteArray = readByteArray;
                }

                //System.out.println("Uploading " + (fragmentNumber == -1 ? "end fragment " : "fragment " + fragmentNumber));
                HttpResponse<String> request = getUnirest().post(endpoint)
                        .queryString("fragNo", fragmentNumber++)
                        .queryString("offset", offset)
                        .contentType("application/octet-stream")
                        .header("Accept", "application/json")
                        .body(sendByteArray)
                        //.uploadMonitor(new FoDProgressMonitor("Import", f.length()))
                        .asString();
                
                System.out.print(".");

                offset += byteCount;

                //System.out.println("Status: " + request.getStatusText());
                // introduce a minor delay otherwise upload stream gets out of sync! :(
                try {
                    Thread.sleep(uploadSyncTime * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                if (request.getStatus() != 202) {
                    // final response has 200, try to deserialize it
                    if (request.getStatus() == 200) {
                        FoDStartScanResponse response = objectMapper.readValue(request.getBody(), FoDStartScanResponse.class);
                        System.out.println("\nUploaded successfully and started scan id: " + response.getScanId() + ". Total bytes sent: "
                                + offset + " (Response: " + request.getStatusText() + ")");
                        return response;
                    } else if (!request.isSuccess()) {
                        System.out.println("An error occurred during the upload.");
                        FoDErrorResponse errors = objectMapper.readValue(request.getBody(), FoDErrorResponse.class);
                        if (errors != null) {
                            if (errors.toString().contains("Can not start another import is in progress")) {
                                System.out.println(errors);
                            } else {
                                System.out.println("Package upload failed for the following reasons: ");
                                System.out.println(errors);
                            }
                        } else {
                            if (!StringUtils.isNotBlank(request.getBody())) System.out.println("Raw response\n" + request.getBody());
                            else System.out.println("No response body from API");
                        }
                        throw new RuntimeException("Error starting scan:"+ (errors != null ? errors.toString() : ""));
                    }
                }

            }

        } catch (Exception ex) {
            throw new RuntimeException("Error starting scan:" + ex.getLocalizedMessage());
        }

        return null;

    }

    //

    private FoDImportScanSessionDescriptor getImportScanSessionDescriptor(String relId) {
        GetRequest request = getUnirest().get(FoDUrls.RELEASE_IMPORT_SCAN_SESSION)
                .routeParam("relId", relId);
        JsonNode session = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(session, FoDImportScanSessionDescriptor.class);
    }

    @RequiredArgsConstructor
    private final class FoDProgressMonitor implements ProgressMonitor {
        private final boolean hasConsole = System.console()!=null;
        private final String action;
        private final Long fileLength;
        private int lineLength=0;

        @Override
        public void accept(String field, String fileName, Long bytesWritten, Long totalBytes) {
            if ( hasConsole ) { // Only output progress when connected to a console, i.e. disable if output is being redirected
                // TODO Should we output to stdout or stderr?
                // TODO Should we use picocli Ansi class instead?
                String msg = String.format("\r%s %s: %d of %d bytes complete          \r", action, fileName, bytesWritten, fileLength);
                lineLength = Math.max(lineLength, msg.length());
                System.out.print(
                        msg
                );
                if ( bytesWritten.equals(totalBytes) ) {
                    // Overwrite the status line with spaces. Alternatively, we could just print a newline to keep the latest progress message
                    System.out.print("\r"+" ".repeat(lineLength)+"\r");
                }
            }
        }
    }
}
