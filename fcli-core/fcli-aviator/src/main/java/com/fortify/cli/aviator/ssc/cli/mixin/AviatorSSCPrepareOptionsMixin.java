package com.fortify.cli.aviator.ssc.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;


@Getter
public class AviatorSSCPrepareOptionsMixin {
    @Option(names = {"--update-template"}, descriptionKey = "fcli.aviator.ssc.prepare.update-template")
    private String templateNameOrId;

    @Option(names = {"--update-all-templates"}, descriptionKey = "fcli.aviator.ssc.prepare.update-all-templates")
    private boolean updateAllTemplates;

    @Option(names = {"--update-av"}, descriptionKey = "fcli.aviator.ssc.prepare.update-av")
    private String appVersionNameOrId;

    @Option(names = {"--update-all-avs"}, descriptionKey = "fcli.aviator.ssc.prepare.update-all-avs")
    private boolean updateAllAvs;
}