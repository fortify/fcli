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
package com.fortify.cli.fpr.error.cli.cmd;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;
import com.fortify.cli.fpr._common.helper.FVDLInfoParser;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-errors")
public class FPRErrorsCommand extends AbstractOutputCommand {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            var info = FVDLInfoParser.parse(fprHandle);
            List<FVDLInfoParser.ErrorEntry> errors = info.engine().errors();

            if (errors.isEmpty()) {
                var row = MAPPER.createObjectNode();
                row.put("code", "");
                row.put("message", "No errors found in the FPR scan results.");
                return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                        .streamSupplier(() -> java.util.stream.Stream.of(row))
                        .build();
            }

            return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                    .streamSupplier(() -> errors.stream().map(err -> {
                        var node = MAPPER.createObjectNode();
                        node.put("code", err.code());
                        node.put("message", err.message());
                        return node;
                    }))
                    .build();
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading FPR file", e);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
