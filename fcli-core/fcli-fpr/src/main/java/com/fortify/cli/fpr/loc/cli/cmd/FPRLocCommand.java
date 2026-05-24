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
package com.fortify.cli.fpr.loc.cli.cmd;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Command(name = "list-source-files", aliases = {"loc", "lsf"})
public class FPRLocCommand extends AbstractOutputCommand {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            var info = FVDLInfoParser.parse(fprHandle);
            var build = info.build();
            var rows = new ArrayList<ObjectNode>();

            for (var file : build.sourceFiles()) {
                var node = MAPPER.createObjectNode();
                node.put("file", file.name());
                node.put("type", file.type());
                node.put("size", file.size());
                node.put("encoding", file.encoding());
                node.put("loc", file.loc() != null ? file.loc() : 0);
                for (var locDetail : file.locDetails()) {
                    if (locDetail.type() != null) {
                        node.put("loc_" + locDetail.type().replace(" ", "_"), locDetail.value());
                    }
                }
                rows.add(node);
            }

            // Summary row with totals
            var totalRow = MAPPER.createObjectNode();
            totalRow.put("file", "TOTAL (" + build.sourceFiles().size() + " files)");
            totalRow.put("type", "");
            totalRow.put("size", "");
            totalRow.put("encoding", "");
            int totalLoc = build.sourceFiles().stream()
                    .mapToInt(f -> f.loc() != null ? f.loc() : 0).sum();
            totalRow.put("loc", totalLoc);
            for (var loc : build.totalLoc()) {
                if (loc.type() != null) {
                    totalRow.put("loc_" + loc.type().replace(" ", "_"), loc.value());
                }
            }
            rows.add(totalRow);

            return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                    .streamSupplier(() -> rows.stream().map(n -> (ObjectNode) n))
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
