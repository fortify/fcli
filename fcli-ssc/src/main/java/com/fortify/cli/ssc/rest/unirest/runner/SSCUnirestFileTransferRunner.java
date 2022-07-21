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
        System.out.println("\nDONE.\n");
        return null;
    }



    @SneakyThrows
    public static Void Upload(UnirestInstance unirestInstance, String url, String filePath){
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
        UploadResponse t1 = responseXml.readValue(r.getBody().toString(), UploadResponse.class);
        System.out.println(t1);
        return null;
    }
}
