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
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.license.ncd_report.cli.mixin.NcdReportListRepositoriesEmbedMixin;
import com.fortify.cli.license.ncd_report.helper.NcdReportRepositoriesOutputHelper;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-repositories", aliases = {"lsr"})
public final class NcdReportListRepositoriesCommand extends AbstractOutputCommand {

    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;

    @Option(names = {"-r", "--report"}, required = true)
    @Getter private Path reportPath;

        @Mixin private NcdReportListRepositoriesEmbedMixin embedMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        var reader = new NcdReportReader(reportPath);
        try {
            return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                    .streamSupplier(() -> readRepositoriesStream(reader))
                    .recordTransformer(n -> enrichRepositoryRecord(n, reader))
                    .build();
        } catch ( RuntimeException e ) {
            reader.close();
            throw e;
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }

    private Stream<ObjectNode> readRepositoriesStream(NcdReportReader reader) {
        return new NcdReportRepositoriesOutputHelper(reader)
                .readRepositoriesAsOutputRows()
                .onClose(reader::close);
    }

    private JsonNode enrichRepositoryRecord(JsonNode record, NcdReportReader reader) {
        return embedMixin.enrichRecord(record, reader);
    }
}
