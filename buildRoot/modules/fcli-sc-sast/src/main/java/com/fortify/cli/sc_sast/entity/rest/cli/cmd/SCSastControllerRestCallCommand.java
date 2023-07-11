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
package com.fortify.cli.sc_sast.entity.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractRestCallCommand;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerProductHelperMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.RestCall.CMD_NAME)
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT) // Output columns depend on response contents
public final class SCSastControllerRestCallCommand extends AbstractRestCallCommand {
    @Getter @Mixin private OutputHelperMixins.RestCall outputHelper;
    @Getter @Mixin private SCSastControllerProductHelperMixin productHelper;
}