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

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.collector.NcdReportRepositoryProcessingResult;
import com.fortify.cli.license.ncd_report.collector.NcdReportRepositoryProcessorMode;
import com.fortify.cli.license.ncd_report.collector.NcdReportRepositorySelectionFilter;
import com.fortify.cli.license.ncd_report.config.NcdReportConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "validate-sources")
public final class NcdReportValidateSourcesCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    @Mixin private UnirestContextMixin unirestContextMixin;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Option(names = {"-c", "--config"}, defaultValue = "NcdReportConfig.yml")
    @Getter private File configFile;

    @Option(names = {"--show"}, defaultValue = "included")
    @Getter private NcdReportRepositorySelectionFilter show;

    @Option(names = {"--limit-per-source"})
    @Getter private Integer limitPerSource;

    @Override
    public JsonNode getJsonNode() {
        var config = loadReportConfig();
        var resultRows = new ArrayList<ObjectNode>();
        try ( var progressWriter = progressWriterFactory.create();
                var reportContext = new NcdReportContext(config, new NoOpReportWriter(), progressWriter,
                        unirestContextMixin.getUnirestContext(), NcdReportRepositoryProcessorMode.SELECTION_ONLY,
                        show, limitPerSource, processingResult -> collectResult(resultRows, processingResult)); )
        {
            var sourceConfigs = config.getSourceConfigs();
            if ( sourceConfigs == null || sourceConfigs.isEmpty() ) {
                throw new FcliSimpleException("Configuration file %s doesn't define any sources", configFile.getAbsolutePath());
            }
            sourceConfigs.forEach(c -> {
                try ( var generator = c.generator(reportContext) ) {
                    generator.run();
                }
            });
        }
        return toOutput(resultRows);
    }

    private NcdReportConfig loadReportConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(configFile, NcdReportConfig.class);
        } catch ( Exception e ) {
            throw new FcliSimpleException(String.format("Error processing configuration file %s:\n\tMessage: %s",
                    configFile.getAbsolutePath(), e.getMessage()));
        }
    }

    private void collectResult(List<ObjectNode> resultRows, NcdReportRepositoryProcessingResult processingResult) {
        if ( !processingResult.processed() || processingResult.status() == null || !show.matches(processingResult.status()) ) {
            return;
        }
        var descriptor = processingResult.repositoryDescriptor();
        var fullName = StringUtils.defaultString(descriptor.getFullName());
        var row = JsonHelper.getObjectMapper().createObjectNode()
                .put("source", StringUtils.defaultString(processingResult.sourceKey()))
                .put("scm", extractScmType(processingResult.sourceKey()))
                .put("scope", extractScope(processingResult.sourceKey()))
                .put("repositoryId", extractRepositoryId(descriptor.asJsonNode(), fullName))
                .put("repositoryName", extractRepositoryName(fullName))
                .put("repositoryFullName", fullName)
                .put("repositoryUrl", StringUtils.defaultString(descriptor.getUrl()))
                .put("visibility", StringUtils.defaultString(descriptor.getVisibility()))
                .put("fork", descriptor.isFork())
                .put("status", processingResult.status().name())
                .put("included", processingResult.status().name().equals("included"))
                .put("reason", StringUtils.defaultString(processingResult.reason()));
        row.set("scmDetails", descriptor.asJsonNode().deepCopy());
        resultRows.add(row);
    }

    private JsonNode toOutput(List<ObjectNode> resultRows) {
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        resultRows.forEach(result::add);
        return result;
    }

    private String extractRepositoryId(JsonNode scmDetails, String fallback) {
        if ( scmDetails == null ) {
            return fallback;
        }
        for ( String idField : List.of("id", "node_id", "uuid") ) {
            var value = scmDetails.path(idField).asText("");
            if ( StringUtils.isNotBlank(value) ) {
                return value;
            }
        }
        return fallback;
    }

    private String extractScmType(String sourceKey) {
        if ( StringUtils.isBlank(sourceKey) || !sourceKey.contains(":")) {
            return "unknown";
        }
        return sourceKey.substring(0, sourceKey.indexOf(':'));
    }

    private String extractScope(String sourceKey) {
        if ( StringUtils.isBlank(sourceKey) || !sourceKey.contains(":")) {
            return "";
        }
        return sourceKey.substring(sourceKey.indexOf(':') + 1);
    }

    private String extractRepositoryName(String repositoryFullName) {
        if ( StringUtils.isBlank(repositoryFullName) || !repositoryFullName.contains("/") ) {
            return repositoryFullName;
        }
        return repositoryFullName.substring(repositoryFullName.lastIndexOf('/') + 1);
    }

    @Override
    public boolean isSingular() {
        return false;
    }

    private static final class NoOpReportWriter implements IReportWriter {
        private final ObjectNode summary = JsonHelper.getObjectMapper().createObjectNode();

        @Override
        public Path absoluteOutputPath() {
            return Paths.get(".").toAbsolutePath();
        }

        @Override
        public ObjectNode summary() {
            return summary;
        }

        @Override
        public BufferedWriter bufferedWriter(String fileName) {
            return new BufferedWriter(new StringWriter());
        }

        @Override
        public IRecordWriter recordWriter(RecordWriterFactory recordWriterFactory, String fileName, boolean isSingular, String options) {
            return NoOpRecordWriter.INSTANCE;
        }

        @Override
        public void copyTextFile(Path source, String targetEntryName) {}

        @Override
        public void close() {}
    }

    private enum NoOpRecordWriter implements IRecordWriter {
        INSTANCE;

        @Override
        public void append(ObjectNode node) {}

        @Override
        public void close() {}
    }
}
