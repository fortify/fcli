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
package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanCompareCommand;
import com.fortify.cli.fod._common.scan.helper.compare.FoDScanCompareSastSupport;
import com.fortify.cli.fod._common.scan.helper.compare.IFoDScanCompareSupport;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.Compare.CMD_NAME)
public class FoDSastScanCompareCommand extends AbstractFoDScanCompareCommand {
    @Getter @Mixin private FoDOutputHelperMixins.Compare outputHelper;
    @Option(names = {"--max-file-changes"}, defaultValue = "1000", paramLabel = "<count>") private int maxFileChanges;
    @Option(names = {"--max-manifest-size"}, defaultValue = "25", paramLabel = "<MB>") private int maxManifestSizeMb;

    @Override
    protected IFoDScanCompareSupport getCompareSupport() {
        return new FoDScanCompareSastSupport(
                requireMinimum("--max-file-changes", maxFileChanges, 0),
                requireMinimum("--max-manifest-size", maxManifestSizeMb, 1));
    }
}
