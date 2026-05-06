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
package com.fortify.cli.aviator._common.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import picocli.CommandLine.Command;

/**
 * Provides Aviator-specific {@link IOutputHelper} implementations for
 * command names not covered by the standard {@link OutputHelperMixins}.
 */
public class AviatorOutputHelperMixins {
    @Command(aliases = {"lss"})
    public static class ListSast extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-sast";
    }

    @Command(aliases = {"lsd"})
    public static class ListDast extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-dast";
    }
}
