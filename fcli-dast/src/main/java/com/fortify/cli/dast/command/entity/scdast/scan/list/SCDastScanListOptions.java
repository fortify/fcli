package com.fortify.cli.dast.command.entity.scdast.scan.list;

import com.fortify.cli.dast.command.entity.types.ScanStatusTypes;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan list
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanListOptions {

    @Option(names = {"-s","--search-text"}, description = "The text to filter results by")
    @Getter private String searchText;

    @Option(names = {"-sd","--start-date"}, description = "The started on start date range")
    @Getter private String startDate;

    @Option(names = {"-ed","--end-date"}, description = "The started on end date range")
    @Getter private String endDate;

    @Option(names = {"-status", "--scan-status"},
            description = "The specific ScanStatusTypes to filter results. Possible values: ${COMPLETION-CANDIDATES}.")
    @Getter private ScanStatusTypes scanStatus;

    @Option(names = {"-ob","--order-by"}, description = "The field name to order the results by")
    @Getter private String orderBy;

    private enum Directions {ASC, DESC}
    @Option(names = {"-od","--order-by-direction"}, description = "The direction to order the results by. Possible values: ${COMPLETION-CANDIDATES}.")
    @Getter private Directions orderByDirection;


}
