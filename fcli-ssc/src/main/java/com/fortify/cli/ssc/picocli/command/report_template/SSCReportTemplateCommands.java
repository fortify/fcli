package com.fortify.cli.ssc.picocli.command.report_template;

import com.fortify.cli.ssc.picocli.command.appversion_artifact.SSCApplicationVersionArtifactDownloadCommand;
import com.fortify.cli.ssc.picocli.command.appversion_artifact.SSCApplicationVersionArtifactListCommand;
import picocli.CommandLine.Command;

@Command(
        name = "report-template",
        aliases = {"rep-templ"},
        subcommands = {
                SSCReportTemplateDownloadCommand.class,
                SSCReportTemplateListCommand.class,
                SSCReportTemplateUploadCommand.class
        },
        description = "Commands for interacting with report template definitions on Fortify SSC."
)
public class SSCReportTemplateCommands {
}
