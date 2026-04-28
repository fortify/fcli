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
package com.fortify.cli.fpr.issue.cli.cmd;

import java.io.IOException;
import java.util.List;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;
import com.fortify.cli.fpr._common.helper.FPRHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FPRIssueListCommand extends AbstractOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        List<Vulnerability> vulnerabilities = loadVulnerabilities();
        return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .streamSupplier(() -> vulnerabilities.stream().map(FPRHelper::toObjectNode))
                .build();
    }

    private List<Vulnerability> loadVulnerabilities() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            return FPRHelper.loadVulnerabilities(fprHandle);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error processing FPR file", e);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
