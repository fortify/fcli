package com.fortify.cli.ssc.picocli.command.report_template;

import picocli.CommandLine.Command;

@Command(
        name = "report-template",
        aliases = {"rep-templ"},
        subcommands = {
                SSCReportTemplateDownloadCommand.class,
                SSCReportTemplateListCommand.class,
                SSCReportTemplateCreateCommand.class
        },
        description = "Commands for interacting with report template definitions on Fortify SSC."
)
public class SSCReportTemplateCommands {
}
