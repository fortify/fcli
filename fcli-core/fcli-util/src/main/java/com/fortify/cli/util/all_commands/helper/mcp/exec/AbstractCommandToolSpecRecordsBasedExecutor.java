/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.all_commands.helper.mcp.exec;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

abstract class AbstractCommandToolSpecRecordsBasedExecutor extends AbstractCommandToolSpecExecutor {
    protected Result collectRecords(String fullCmd, ArrayNode records) {
        return FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .stdoutOutputType(OutputType.suppress)
            .stderrOutputType(OutputType.collect)
            .recordConsumer(records::add)
            .onFail(r->{}) // Continue on non-zero exit code, assuming stdout/stderr shows more info about the error, which in turn can be
                           //  used by the LLM to provide suggestions on how to fix.
            .build().create().execute();
    }
}