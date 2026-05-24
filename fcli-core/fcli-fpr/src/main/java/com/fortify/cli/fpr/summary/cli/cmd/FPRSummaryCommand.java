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
package com.fortify.cli.fpr.summary.cli.cmd;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;
import com.fortify.cli.fpr._common.helper.FVDLInfoParser;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "show-summary", aliases = {"summary", "sum"})
public class FPRSummaryCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Override
    public JsonNode getJsonNode() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            var info = FVDLInfoParser.parse(fprHandle);
            var build = info.build();
            var engine = info.engine();
            var node = MAPPER.createObjectNode();

            node.put("project", build.project());
            node.put("version", build.version());
            node.put("buildID", build.buildID());
            node.put("numberFiles", build.numberFiles());
            node.put("sourceBasePath", build.sourceBasePath());
            if (build.scanTimeSeconds() != null) {
                node.put("scanTimeSeconds", build.scanTimeSeconds());
            }
            if (build.buildDuration() != null) {
                node.put("buildDurationSeconds", build.buildDuration());
            }

            if (!build.totalLoc().isEmpty()) {
                var locNode = MAPPER.createObjectNode();
                for (var loc : build.totalLoc()) {
                    locNode.put(loc.type() != null ? loc.type() : "total", loc.value());
                }
                node.set("loc", locNode);
            }

            node.put("engineVersion", engine.engineVersion());

            if (engine.machineInfo() != null) {
                var mi = MAPPER.createObjectNode();
                mi.put("hostname", engine.machineInfo().hostname());
                mi.put("username", engine.machineInfo().username());
                mi.put("platform", engine.machineInfo().platform());
                node.set("machineInfo", mi);
            }

            if (!engine.rulePacks().isEmpty()) {
                ArrayNode rp = MAPPER.createArrayNode();
                for (var pack : engine.rulePacks()) {
                    var p = MAPPER.createObjectNode();
                    p.put("name", pack.name());
                    p.put("version", pack.version());
                    p.put("id", pack.id());
                    if (pack.sku() != null) { p.put("sku", pack.sku()); }
                    rp.add(p);
                }
                node.set("rulePacks", rp);
            }

            if (!engine.commandLine().isEmpty()) {
                ArrayNode cmdNode = MAPPER.createArrayNode();
                engine.commandLine().forEach(cmdNode::add);
                node.set("commandLine", cmdNode);
            }

            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading FPR file", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
