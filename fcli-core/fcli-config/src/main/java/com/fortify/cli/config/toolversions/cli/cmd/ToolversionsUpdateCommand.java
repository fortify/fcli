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
package com.fortify.cli.config.toolversions.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name=OutputHelperMixins.Update.CMD_NAME)
public class ToolversionsUpdateCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    @Mixin @Getter private OutputHelperMixins.Update outputHelper;
    @Getter @Option(names={"--url"}, required = false, descriptionKey="fcli.config.toolversions.update.url") 
    private String pUrl;
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public JsonNode getJsonNode() {
        //TODO replace with final package url
        String packageUrl = "https://drive.google.com/uc?export=download&id=17oFvD5FO10FOAWlMQPHJlp9PPWDojg8J";
        if(pUrl!=null && !pUrl.isBlank()) {
            packageUrl = pUrl;
        }
        try {
            return downloadAndUnpack(packageUrl);
        } catch ( IOException e ) {
            throw new RuntimeException("Error updating toolversions", e);
        }
    }
    
    private JsonNode downloadAndUnpack(String packageUrl) throws IOException{
        Path toolversionsPath = FcliDataHelper.getFcliConfigPath().resolve("toolversions");
        Files.createDirectories(toolversionsPath);
        File pkg = download(packageUrl);
        FileUtils.extractZip(pkg, toolversionsPath);
        
        ArrayNode result = objectMapper.createArrayNode();
        try {
            if ( toolversionsPath.toFile().exists() ) {
                Files.walk(toolversionsPath)
                    .sorted(Comparator.reverseOrder())
                    .filter(f->f.toFile().isFile())
                    .map(Path::toFile)
                    .forEach(f->addResult(result,f));
            }
        } catch ( IOException e ) {
            throw new RuntimeException("Error listing unpacked tool version files", e);
        }
        return result;
    }
    
    private final File download(String packageUrl) throws IOException {
        File tempDownloadFile = File.createTempFile("fcli-toolversions-download", null);
        tempDownloadFile.deleteOnExit();
        download(packageUrl, tempDownloadFile);
        return tempDownloadFile;
    }
    
    private final Void download(String downloadUrl, File destFile) {
        UnirestInstance unirest = GenericUnirestFactory.getUnirestInstance("toolversions",
                u->ProxyHelper.configureProxy(u, "toolversions", downloadUrl));
        unirest.get(downloadUrl).asFile(destFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
        return null;
    }
    
    private void addResult(ArrayNode result, File f) {
        try {
            result.add(objectMapper.createObjectNode()
                    .put("name", f.getCanonicalPath())
                    .put("type", "FILE"));
        } catch ( IOException e ) {
            throw new RuntimeException("Error processing file "+f, e);
        }
    }
}
