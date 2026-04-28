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
package com.fortify.cli.fpr.remediation.cli.cmd;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class FPRApplyRemediationsCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Option(names = {"--source-root"}, required = true, order = 2)
    private Path sourceRoot;

    @Override
    public ObjectNode getJsonNode() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            if (!fprHandle.hasRemediations()) {
                throw new FcliSimpleException("FPR does not contain remediations.xml; no remediations to apply");
            }
            var processor = new RemediationProcessor(fprHandle, sourceRoot.toAbsolutePath().toString());
            var metric = processor.processRemediationXML();
            var result = MAPPER.createObjectNode();
            result.put("totalRemediations", metric.totalRemediations());
            result.put("appliedRemediations", metric.appliedRemediations());
            result.put("skippedRemediations", metric.skippedRemediations());
            result.put("__action__", metric.appliedRemediations() > 0 ? "APPLIED" : "SKIPPED");
            return result;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error processing FPR file", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
