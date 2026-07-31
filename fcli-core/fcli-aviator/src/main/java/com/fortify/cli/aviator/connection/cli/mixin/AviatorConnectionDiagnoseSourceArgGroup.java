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
package com.fortify.cli.aviator.connection.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Exclusive diagnose source: URL (optional token), saved user session, or admin config.
 */
@Getter
public class AviatorConnectionDiagnoseSourceArgGroup {
    @ArgGroup(exclusive = false, multiplicity = "0..1", order = 1)
    private AviatorConnectionDiagnoseUrlSourceArgGroup urlSource;

    @Option(names = {"--aviator-session", "--av-session"}, order = 2)
    private String aviatorSession;

    @Option(names = {"--admin-config"}, order = 3)
    private String adminConfig;
}
