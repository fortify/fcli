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
package com.fortify.cli.aviator.connection.cli.cmd;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.connection.cli.mixin.AviatorConnectionDiagnoseSourceArgGroup;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "diagnose")
public class AviatorConnectionDiagnoseCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;

    @ArgGroup(exclusive = true, multiplicity = "1", headingKey = "aviator.connection.diagnose.source.arggroup")
    private AviatorConnectionDiagnoseSourceArgGroup sourceArgGroup;

    @Option(names = "--timeout", defaultValue = "30", paramLabel = "<seconds>")
    private int timeoutSeconds;

    private final AviatorConnectionDiagnoseHelper diagnoseHelper = new AviatorConnectionDiagnoseHelper();
    private List<AviatorDiagnosticStageResult> lastResults;

    @Override
    public Integer call() {
        var output = getJsonNode();
        getOutputHelper().write(simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC).source(output).build());
        return diagnoseHelper.hasRequiredFailure(lastResults) ? 1 : 0;
    }

    @Override
    public JsonNode getJsonNode() {
        if (timeoutSeconds <= 0) {
            throw new FcliSimpleException("--timeout must be greater than 0");
        }
        lastResults = diagnoseHelper.diagnose(sourceArgGroup, timeoutSeconds);
        return diagnoseHelper.toArrayNode(lastResults);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}