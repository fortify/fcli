package com.fortify.cli.tool._common.cli.mixin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.tool._common.helper.ToolDefinitionsStateDescriptor;
import com.fortify.cli.tool._common.helper.ToolHelper;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ToolDefinitionsUpdateMixin {
    // When updating this URL, please also update the URL in the resource bundle
    private static final String DEFAULT_URL = "https://github.com/fortify-ps/tool-definitions/raw/main/v1/tool-definitions.yaml.zip";
    private static final DateTimePeriodHelper periodHelper = DateTimePeriodHelper.byRange(Period.SECONDS, Period.DAYS);
    private static final Path DESCRIPTOR_PATH = ToolHelper.DEFINITIONS_STATE_DIR.resolve("state.json");
    @Getter @Option(names={"-s", "--definitions-source"}, required = false, descriptionKey="fcli.tool.definitions.update.definitions-source") 
    private String source = DEFAULT_URL;
    @Getter @Option(names={"-a", "--max-definitions-age"}, required = false, defaultValue = "1h", descriptionKey="fcli.tool.definitions.update.max-definitions-age") 
    private String maxAge;
    
    public final ToolDefinitionsStateDescriptor updateToolDefinitions() throws IOException {
        createDefinitionsStateDir(ToolHelper.DEFINITIONS_STATE_DIR);
        var zip = ToolHelper.DEFINITIONS_STATE_ZIP;
        ToolDefinitionsStateDescriptor descriptor = FcliDataHelper.readFile(DESCRIPTOR_PATH, ToolDefinitionsStateDescriptor.class, false);
        if ( descriptor==null || !Files.exists(zip) || isMaxAgeExpired(descriptor, maxAge) ) {
            update(source, zip);
            descriptor = new ToolDefinitionsStateDescriptor(source, new Date(getModifiedTime(zip).toMillis()), "UPDATED");
        } else {
            descriptor = new ToolDefinitionsStateDescriptor(descriptor.getSource(), descriptor.getLastUpdate(), "SKIPPED");
        }
        FcliDataHelper.saveFile(DESCRIPTOR_PATH, descriptor, true);
        return descriptor;
    }
    
    private static final void createDefinitionsStateDir(Path dir) throws IOException {
        if( !Files.exists(dir) ) {
            Files.createDirectories(dir);
        }
    }
    
    private static final boolean isMaxAgeExpired(ToolDefinitionsStateDescriptor descriptor, String maxAge) {
        var lastUpdate = descriptor.getLastUpdate().toInstant();
        return periodHelper.getCurrentOffsetDateTimeMinusPeriod(maxAge).toInstant().compareTo(lastUpdate)>0;
    }

    private static FileTime getModifiedTime(Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.lastModifiedTime();
    }
    
    private static final void update(String source, Path dest) throws IOException {
        try {
            ToolHelper.download(new URL(source).toString(), dest.toFile());
        } catch ( MalformedURLException e ) {
            Files.copy(Path.of(source), dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
