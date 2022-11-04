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
import java.util.Arrays;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.cli.mixin.FoDImportScanSessionDescriptor;
import com.fortify.cli.fod.scan.helper.FoDImportScanResponse;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.*;
import lombok.*;

import javax.validation.ValidationException;

@ReflectiveAccess
public class FoDFileTransferHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    private static final int CHUNK_SIZE = 1024 * 1024;

    @SneakyThrows
    public static FoDImportScanResponse importScan(UnirestInstance unirest, String relId, String endpoint, String filePath) {

        byte[] readByteArray = new byte[CHUNK_SIZE];
        byte[] sendByteArray;
        int fragmentNumber = 0;
        int byteCount;
        long offset = 0;

        File f = new File(filePath);
        if (!f.exists() || !f.canRead())
            throw new ValidationException("Could not read file: " + f.getPath());
        System.out.println("File: " + f.getPath() + "; size: " + f.length());

        String importScanSessionId = getImportScanSessionDescriptor(unirest, relId).getImportScanSessionId();
        System.out.println("Import scan session id: " + importScanSessionId);

        try (FileInputStream fs = new FileInputStream(filePath)) {

            // Loop through chunks
            while ((byteCount = fs.read(readByteArray)) != -1) {
                if (byteCount < CHUNK_SIZE) {
                    sendByteArray = Arrays.copyOf(readByteArray, byteCount);
                    fragmentNumber = -1;
                } else {
                    sendByteArray = readByteArray;
                }
                String fragUrl = endpoint + "?fragNo=" + fragmentNumber++
                        + "&offset=" + offset
                        + "&fileLength=" + byteCount
                        + "&importScanSessionId=" + importScanSessionId;
                System.out.println(fragUrl);

                HttpResponse<String> request = unirest.put(fragUrl)
                        .routeParam("relId", relId)
                        .body(sendByteArray)
                        .uploadMonitor(new FoDProgressMonitor("Import", f.length()))
                        .asString();

                offset += byteCount;

                if (request.getStatus() != 202) {
                    if (request.getStatus() == 200) {
                        return objectMapper.readValue(request.getBody(), FoDImportScanResponse.class);
                    } else if (request.getStatus() == 400 || request.getStatus() == 500) {
                        FoDErrorResponse errors = objectMapper.readValue(request.getBody(), FoDErrorResponse.class);
                        throw new RuntimeException("Error importing scan:" + errors.toString());
                    } else {
                        throw new RuntimeException("Error importing scan: " + request.getStatusText());
                    }
                }

            }

        } catch (Exception ex) {
            throw new RuntimeException("Error importing scan:" + ex.getLocalizedMessage());
        }

        return null;

    }

    private static FoDImportScanSessionDescriptor getImportScanSessionDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.RELEASE_IMPORT_SCAN_SESSION)
                .routeParam("relId", relId);
        JsonNode session = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(session, FoDImportScanSessionDescriptor.class);
    }

    @RequiredArgsConstructor
    private static final class FoDProgressMonitor implements ProgressMonitor {
        private static final boolean hasConsole = System.console()!=null;
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
