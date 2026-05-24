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
package com.fortify.cli.fpr.signature.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;
import com.fortify.cli.fpr._common.helper.FVDLInfoParser;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "show-signature", aliases = {"signature", "sign"})
public class FPRSignatureCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Override
    public JsonNode getJsonNode() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            var node = MAPPER.createObjectNode();

            // Read VERSION file
            Path versionPath = fprHandle.getPath("/VERSION");
            if (Files.exists(versionPath)) {
                node.put("fprVersion", Files.readString(versionPath).trim());
            }

            // Read MAC file
            Path macPath = fprHandle.getPath("/audit.fvdl.mac");
            if (Files.exists(macPath)) {
                byte[] macBytes = Files.readAllBytes(macPath);
                node.put("mac", bytesToHex(macBytes));
                node.put("signed", true);
            } else {
                node.put("signed", false);
            }

            // Engine version from FVDL
            var info = FVDLInfoParser.parse(fprHandle);
            if (info.engine().engineVersion() != null) {
                node.put("engineVersion", info.engine().engineVersion());
            }
            if (info.build().buildID() != null) {
                node.put("buildID", info.build().buildID());
            }
            if (info.engine().machineInfo() != null) {
                node.put("hostname", info.engine().machineInfo().hostname());
                node.put("username", info.engine().machineInfo().username());
                node.put("platform", info.engine().machineInfo().platform());
            }

            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading FPR file", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
