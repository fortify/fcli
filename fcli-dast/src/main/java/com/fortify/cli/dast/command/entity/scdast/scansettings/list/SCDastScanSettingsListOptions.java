package com.fortify.cli.dast.command.entity.scdast.scansettings.list;

import com.fortify.cli.common.command.auth.login.AuthLoginCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan Settings
 * Usually this would be included in a {@link AuthLoginCommand} implementation
 * as follows:
 * <pre>
 * {@code
 *   {@literal @}ArgGroup(exclusive = false, multiplicity = "1", heading = "<System> Scan Settings Options:%n")
 *   {@literal @}Getter private SCDASTScanSettingsOptions conn;
 * }
 * </pre>
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanSettingsListOptions {

    @Option(names = {"-s","--search-text"}, description = "The text to filter results by.")
    @Getter private String searchText;

    @Option(names = {"-sd","--start-date"}, description = "The modified start date range.")
    @Getter private String startDate;

    @Option(names = {"-ed","--end-date"}, description = "The modified end date range.")
    @Getter private String endDate;

    @Option(names = {"-t","--scan-type"}, description = "The specific ScanTypes to filter results. [ Standard, WorkflowDriven, AMI ]")
    @Getter private String scanType; //TODO: set to enum
}
