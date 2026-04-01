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
package com.fortify.cli.sc_dast.scan.cli.cmd.action;

import java.util.Collections;
import java.util.Map;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class SCDastScanDeleteCommand extends AbstractSCDastScanActionCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Option(names = {"--force-delete", "-f"}, description = "Force deletion of the scan by adding forceDelete=true query parameter")
    private boolean forceDelete;

    @Override
    protected SCDastScanAction getAction() {
        return SCDastScanAction.DeleteScan;
    }

    @Override
    protected Map<String, Object> getQueryParameters() {
        if (forceDelete) {
            return Map.of("forceDelete", "true");
        }
        return Collections.emptyMap();
    }
}
