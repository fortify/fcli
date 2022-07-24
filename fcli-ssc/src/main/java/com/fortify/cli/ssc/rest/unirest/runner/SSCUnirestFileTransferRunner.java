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
package com.fortify.cli.ssc.rest.unirest.runner;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.ssc.common.pojos.uploadResponse.UploadResponse;
import com.jayway.jsonpath.JsonPath;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import java.io.File;
import java.nio.file.StandardCopyOption;

public class SSCUnirestFileTransferRunner {

    private enum FileTransferTokenType{
        UPLOAD,
        DOWNLOAD
    }
    private static String getFileTransferToken(UnirestInstance unirest, FileTransferTokenType tokenType){
        String t = tokenType == FileTransferTokenType.UPLOAD ? "UPLOAD" : "DOWNLOAD";
        HttpResponse response = unirest.post("/api/v1/fileTokens")
                .body(String.format("{ \"fileTokenType\": \"%s\"}", t))
                .accept("application/json")
                .contentType("application/json")
                .asObject(ObjectNode.class);
        return JsonPath.parse(response.getBody().toString()).read("$.data.token").toString();
    }

    @SneakyThrows
    public static Void Download(UnirestInstance unirestInstance, String url, String downloadPath){
        String downloadToken = getFileTransferToken(unirestInstance, FileTransferTokenType.DOWNLOAD);
        unirestInstance.get(url)
                .routeParam("downloadToken",downloadToken)
                .downloadMonitor((b, filename, bytesWritten, totalBytes) -> {
                    String msg = String.format("\rBytes written for \"%s\": %d    ", filename, bytesWritten);
                    System.out.print(msg);
                })
                .asFile(downloadPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("\nDOWNLOAD DONE.\n");
        return null;
    }



    @SneakyThrows
    public static UploadResponse Upload(UnirestInstance unirestInstance, String url, String filePath){
        String uploadToken = getFileTransferToken(unirestInstance, FileTransferTokenType.UPLOAD);
        File f = new File(filePath);
        //InputStream file = new FileInputStream(f); // Supposedly this should be used for larger file uploads, but SSC errors when using this.

        HttpResponse r = Unirest.post(unirestInstance.config().getDefaultBaseUrl() + url)
                .routeParam("uploadToken",uploadToken)
                .field("file", f)
                .uploadMonitor((field, fileName, bytesWritten, totalBytes) -> {
                    String msg = String.format("\rBytes uploaded for for \"%s\": %d    \r", f.getName(), bytesWritten);
                    System.out.print(msg);
                })
                .asString();
        XmlMapper responseXml = new XmlMapper(new JacksonXmlModule());
        UploadResponse uploadResponseObj = responseXml.readValue(r.getBody().toString(), UploadResponse.class);
        System.out.println("\nUPLOAD DONE.\n");
        return uploadResponseObj;
    }
}
