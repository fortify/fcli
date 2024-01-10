package com.fortify.cli.tool.config.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ToolConfigUriMixin {
    @Getter @Option(names={"--url"}, required = false, descriptionKey="fcli.tool.config.update.url") 
    private String _url;
    @Getter @Option(names={"--file"}, required = false, descriptionKey="fcli.tool.config.update.file") 
    private String _file;
}
