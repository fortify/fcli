package com.fortify.cli.ssc.rest.unirest.runner;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;

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
                    String msg = String.format("\rBytes written for \"%s\": %d    \r", filename, bytesWritten);
                    System.out.print(msg);
                })
                .asFile(downloadPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("");
        return null;
    }

    @SneakyThrows
    public static Void Upload(UnirestInstance unirestInstance, String url, String filePath){
        String uploadToken = getFileTransferToken(unirestInstance, FileTransferTokenType.UPLOAD);
        unirestInstance.post(url)
                .routeParam("uploadToken",uploadToken)
                .downloadMonitor((b, filename, bytesWritten, totalBytes) -> {
                    String msg = String.format("\rBytes written for \"%s\": %d    \r", filename, bytesWritten);
                    System.out.print(msg);
                });
        return null;
    }
}
