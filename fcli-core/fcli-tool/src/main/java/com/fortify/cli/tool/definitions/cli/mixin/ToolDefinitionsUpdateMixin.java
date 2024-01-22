package com.fortify.cli.tool.definitions.cli.mixin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsStateDescriptor;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ToolDefinitionsUpdateMixin {
    private static final String DEFAULT_URL = "https://github.com/fortify-ps/tool-definitions/raw/main/v1/tool-definitions.yaml.zip";
    private static final Path DESCRIPTOR_PATH = ToolDefinitionsHelper.DEFINITIONS_STATE_DIR.resolve("state.json");
    @Getter @Option(names={"-s", "--source"}, required = false, descriptionKey="fcli.tool.definitions.update.definitions-source") 
    private String source = DEFAULT_URL;
    
    public final ToolDefinitionsStateDescriptor updateToolDefinitions() throws IOException {
        createDefinitionsStateDir(ToolDefinitionsHelper.DEFINITIONS_STATE_DIR);
        var zip = ToolDefinitionsHelper.DEFINITIONS_STATE_ZIP;
        var descriptor = update(source, zip);
        FcliDataHelper.saveFile(DESCRIPTOR_PATH, descriptor, true);
        return descriptor;
    }
    
    private static final void createDefinitionsStateDir(Path dir) throws IOException {
        if( !Files.exists(dir) ) {
            Files.createDirectories(dir);
        }
    }

    private static FileTime getModifiedTime(Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.lastModifiedTime();
    }
    
    private static final ToolDefinitionsStateDescriptor update(String source, Path dest) throws IOException {
        try {
            UnirestHelper.download("tool", new URL(source).toString(), dest.toFile());
        } catch ( MalformedURLException e ) {
            Files.copy(Path.of(source), dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return new ToolDefinitionsStateDescriptor(source, new Date(getModifiedTime(dest).toMillis()));
    }
}
