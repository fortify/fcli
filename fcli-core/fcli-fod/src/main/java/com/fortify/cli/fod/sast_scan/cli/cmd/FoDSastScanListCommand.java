/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod.scan.cli.cmd.FoDScanListCommand;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import picocli.CommandLine.Command;

import java.util.function.Predicate;

@Command(name = OutputHelperMixins.List.CMD_NAME, hidden = false)
public class FoDSastScanListCommand extends FoDScanListCommand implements IRecordTransformer {

    @Override
    protected Predicate<JsonNode> getFilterPredicate() {
        Predicate<JsonNode> result = o->true;
        result = and(result, "scanType", FoDScanType.Static);
        result = and(result, "analysisStatusType", super.getStatus());
        return result;
    }
}
