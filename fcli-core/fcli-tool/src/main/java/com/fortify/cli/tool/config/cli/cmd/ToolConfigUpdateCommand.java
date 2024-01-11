package com.fortify.cli.tool.config.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.tool.config.cli.mixin.ToolConfigSourceMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.Update.CMD_NAME)
public class ToolConfigUpdateCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier{
    @Mixin @Getter private OutputHelperMixins.Update outputHelper;
    @ArgGroup(exclusive=true, multiplicity="0..1")
    private ToolConfigSourceMixin toolConfigSourceMixin;
    private String defaultDownloadUrl= "https://github.com/fortify-ps/tool-definitions/raw/main/v1/tool-definitions.yaml.zip";
    
    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public JsonNode getJsonNode() {
        try {
            return new ObjectMapper().<ObjectNode>valueToTree(updateToolDefinitions());
        } catch (IOException e) {
            throw new RuntimeException("Error updating tool definitions", e);
        }
    }
    
    private ToolBundleDownloadDescriptor updateToolDefinitions() throws IOException {
        if(!getZipPath().toFile().exists()) {
            Files.createDirectories(getZipPath());
        }
        if(toolConfigSourceMixin!=null) {
            if(toolConfigSourceMixin.getFile()!=null) {
                return getFromLocalPath(toolConfigSourceMixin.getFile());
            }
            if(toolConfigSourceMixin.getUrl()!=null) {
                return downloadFromWeblink(toolConfigSourceMixin.getUrl());
            }
        }
        return downloadFromWeblink(defaultDownloadUrl);
    }
    
    private ToolBundleDownloadDescriptor downloadFromWeblink(String url) throws IOException {
        File pkg = download(url, getZipPath().toFile());
        return new ToolBundleDownloadDescriptor(url, pkg.getPath());
    }
    
    private ToolBundleDownloadDescriptor getFromLocalPath(String path) throws IOException {
        Path pkg = Files.copy(Path.of(path), getZipPath(), StandardCopyOption.REPLACE_EXISTING);
        return new ToolBundleDownloadDescriptor(path, pkg.toString());
    }

    private final File download(String downloadUrl, File destFile) {
        UnirestInstance unirest = GenericUnirestFactory.getUnirestInstance("toolversions",
                u->ProxyHelper.configureProxy(u, "toolversions", downloadUrl));
        unirest.get(downloadUrl).asFile(destFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
        return destFile;
    }
    
    private Path getZipPath() {
        return FcliDataHelper.getFcliConfigPath().resolve("tool/tool-definitions.yaml.zip");
    }
    
    private class ToolBundleDownloadDescriptor{
        public String remotePath;
        public String localPath;
        public ToolBundleDownloadDescriptor(String remotePath, String localPath) {
            this.remotePath = remotePath;
            this.localPath = localPath;
        }
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
}
