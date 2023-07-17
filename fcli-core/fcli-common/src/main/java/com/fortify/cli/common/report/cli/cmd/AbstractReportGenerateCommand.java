/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.report.cli.cmd;

import java.io.File;
import java.util.Date;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.report.writer.ReportDirWriter;
import com.fortify.cli.common.report.writer.ReportZipWriter;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.StringUtils;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * <p>Base class for commands that can generate reports. This base class provides
 * functionality for specifying report output location through CLI options, and
 * generating some generic report summary contents like report type and date.</p>
 * 
 * <p>Note that there's a more specific base class for reporting commands that
 * take a configuration file as input; see {@link AbstractConfigurableReportGenerateCommand}.</p>
 * 
 * @author rsenden
 *
 */
public abstract class AbstractReportGenerateCommand extends AbstractOutputCommand implements IJsonNodeSupplier
{
    @ArgGroup(exclusive = true, multiplicity = "1") private OutputArgGroup outputArgGroup;
    private static final class OutputArgGroup {
        @Option(names = {"-f", "--report-zip"}, required = true) private String reportZipName;   
        @Option(names = {"-d", "--report-dir"}, required = true) private String reportDirName;
    }
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    @Mixin private CommandHelperMixin commandHelper;
    
    @Override
    public JsonNode getJsonNode() {
        ObjectNode output = JsonHelper.getObjectMapper().createObjectNode();
        try (var reportWriter = createReportWriter() ) {
            reportWriter.summary()
                .put("reportType", getReportTitle())
                .put("reportDate", new Date().toString())
                .put("generatedBy", FcliBuildPropertiesHelper.getFcliBuildInfo());
            output.put("reportPath", reportWriter.absoluteOutputPath().toString());
            generateReport(reportWriter);
            output.set("summary", reportWriter.summary());
        }
        return output;
    }
    
    private IReportWriter createReportWriter() {
        if ( StringUtils.isNotBlank(outputArgGroup.reportZipName) ) {
            deleteExisting(outputArgGroup.reportZipName, File::delete);
            return new ReportZipWriter(outputArgGroup.reportZipName, commandHelper.getMessageResolver());
        } else if ( StringUtils.isNotBlank(outputArgGroup.reportDirName) ) {
            deleteExisting(outputArgGroup.reportDirName, this::deleteDirectory);
            return new ReportDirWriter(outputArgGroup.reportDirName, commandHelper.getMessageResolver());
        } else {
            throw new IllegalArgumentException("Either --report-file or --report-dir must be specified");
        }
    }   
    
    private void deleteExisting(String name, Consumer<File> deleter) {
        var file = new File(name); 
        if ( file.exists() ) {
            requireConfirmation.checkConfirmed(name);
            deleter.accept(file);
        }
    }
    
    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
    
    protected abstract String getReportTitle();
    
    protected abstract void generateReport(IReportWriter reportWriter);
}
