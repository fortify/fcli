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


import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import kong.unirest.HttpRequestWithBody;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class SCDastScanDeleteCommand extends AbstractSCDastScanActionCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Option(names = {"--force", "-f"})
    private boolean forceDelete;

    @Override
    protected SCDastScanAction getAction() {
        return SCDastScanAction.DeleteScan;
    }

    @Override
    protected HttpRequestWithBody updateRequest(HttpRequestWithBody request) {
        return request.queryString("forceDelete", forceDelete);
    }
}
