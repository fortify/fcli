package com.fortify.cli.sc_dast.command.crud.scansettings;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IOutputOptionsWriterConfigSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands.SCDastGetCommand;
import com.fortify.cli.sc_dast.command.crud.scansettings.options.SCDastGetScanSettingsListOptions;
import com.fortify.cli.sc_dast.command.crud.scansettings.options.SCDastGetScanSettingsOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanSettingsCommands {
    private static final String NAME = "scan-settings";
    private static final String DESC = "DAST scan settings";

    private static final String _getDefaultOutputColumns() { return "id#name#cicdToken#applicationName#applicationVersionName"; }


    @ReflectiveAccess
    @SubcommandOf(SCDastCrudRootCommands.SCDastGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IOutputOptionsWriterConfigSupplier {
        @ArgGroup(exclusive = false, heading = "Get a specific scan settings:%n", order = 1)
        @Getter private SCDastGetScanSettingsOptions scanSettingsOptions;

        @ArgGroup(exclusive = false, heading = "Filter multiple scan settings:%n", order = 2)
        @Getter private SCDastGetScanSettingsListOptions scanSettingsListOptions;


        @Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/application-version-scan-settings/scan-settings-summary-list";
            String urlParams = "";

            if (scanSettingsOptions != null){
                urlPath = "/api/v2/application-version-scan-settings/" + scanSettingsOptions.getScanSettingsId();
            } else {
                if(scanSettingsListOptions != null){
                    if (scanSettingsListOptions.getSearchText() != null){
                        urlParams += String.format("searchText=%s&",scanSettingsListOptions.getSearchText());
                    }
                    if(scanSettingsListOptions.getStartDate() != null){
                        urlParams += String.format("modifiedStartDate=%s&",scanSettingsListOptions.getStartDate());
                    }
                    if(scanSettingsListOptions.getEndDate() != null){
                        urlParams += String.format("modifiedEndDate=%s&",scanSettingsListOptions.getEndDate());
                    }
                    if(scanSettingsListOptions.getScanType() != null){
                        urlParams += String.format("scanType=%s&",scanSettingsListOptions.getScanType());
                    }
                }
            }

            outputOptionsHandler.write(unirest.get(urlPath + "?" + urlParams)
                    .accept("application/json")
                    .header("Content-Type", "application/json"));

            return null;
        }

        @Override
        public OutputOptionsWriterConfig getOutputOptionsWriterConfig() {
            return SCDastGetCommand.defaultOutputConfig().defaultColumns(_getDefaultOutputColumns());
        }
    }
}

