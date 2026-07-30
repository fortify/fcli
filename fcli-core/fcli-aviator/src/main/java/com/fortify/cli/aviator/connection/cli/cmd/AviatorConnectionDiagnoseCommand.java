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

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator.connection.cli.mixin.AviatorConnectionDiagnoseSourceArgGroup;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseHelper;
import com.fortify.cli.aviator.connection.helper.AviatorConnectionDiagnoseSource;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "diagnose")
public class AviatorConnectionDiagnoseCommand extends AbstractOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;

    @ArgGroup(exclusive = true, multiplicity = "1", headingKey = "aviator.connection.diagnose.source.arggroup")
    private AviatorConnectionDiagnoseSourceArgGroup sourceArgGroup;

    @Option(names = "--timeout", defaultValue = "30", paramLabel = "<seconds>")
    private int timeoutSeconds;

    /**
     * Runs diagnostics and returns a producer that writes the stage table and
     * reports exit code 1 when a required stage failed (soft exit after output).
     */
    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        if (timeoutSeconds <= 0) {
            throw new FcliSimpleException("--timeout must be greater than 0");
        }
        var runResult = new AviatorConnectionDiagnoseHelper().diagnose(resolveSource(), timeoutSeconds);
        return simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .source(runResult.json())
                .exitCode(runResult.requiredFailure() ? 1 : 0)
                .build();
    }

    private AviatorConnectionDiagnoseSource resolveSource() {
        var urlSource = sourceArgGroup.getUrlSource();
        if (urlSource != null) {
            var token = urlSource.getTokenOrNull();
            return token != null
                ? AviatorConnectionDiagnoseSource.fromUrlAndToken(urlSource.getUrl(), token)
                : AviatorConnectionDiagnoseSource.fromUrl(urlSource.getUrl());
        }
        var sessionName = sourceArgGroup.getAviatorSession();
        if (sessionName != null) {
            return AviatorConnectionDiagnoseSource.fromUserSession(
                AviatorUserSessionHelper.instance().get(sessionName, true));
        }
        var adminName = sourceArgGroup.getAdminConfig();
        if (adminName != null) {
            return AviatorConnectionDiagnoseSource.fromAdminConfig(
                AviatorAdminConfigHelper.instance().get(adminName, true));
        }
        throw new FcliBugException("No diagnose source selected; exclusive ArgGroup invariant was violated");
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
