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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator.connection.cli.mixin.AviatorConnectionDiagnoseSourceArgGroup;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper.DiagnoseRunResult;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource;
import com.fortify.cli.common.exception.FcliSimpleException;
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
    private DiagnoseRunResult runResult;

    @Override
    public Integer call() {
        getOutputHelper().write(getObjectNodeProducer());
        return runResult.requiredFailure() ? 1 : 0;
    }

    @Override
    public JsonNode getJsonNode() {
        if (timeoutSeconds <= 0) {
            throw new FcliSimpleException("--timeout must be greater than 0");
        }
        if (runResult == null) {
            runResult = diagnoseHelper.diagnose(resolveSource(), timeoutSeconds);
        }
        return runResult.json();
    }

    private AviatorConnectionDiagnoseSource resolveSource() {
        if (sourceArgGroup.getUrl() != null) {
            return AviatorConnectionDiagnoseSource.fromUrl(sourceArgGroup.getUrl());
        }
        if (sourceArgGroup.getAviatorSession() != null) {
            var descriptor = AviatorUserSessionHelper.instance().get(sourceArgGroup.getAviatorSession(), true);
            return AviatorConnectionDiagnoseSource.fromUserSession(descriptor);
        }
        var descriptor = AviatorAdminConfigHelper.instance().get(sourceArgGroup.getAdminConfig(), true);
        return AviatorConnectionDiagnoseSource.fromAdminConfig(descriptor);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
