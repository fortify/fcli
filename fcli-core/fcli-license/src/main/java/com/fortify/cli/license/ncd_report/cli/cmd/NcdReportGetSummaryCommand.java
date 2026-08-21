/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.ncd_report.cli.cmd;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "get-summary")
public final class NcdReportGetSummaryCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Option(names = {"-r", "--report"}, required = true)
    @Getter private Path reportPath;

    @Override
    public JsonNode getJsonNode() {
        try ( var reader = new NcdReportReader(reportPath) ) {
            return reader.readSummary();
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
