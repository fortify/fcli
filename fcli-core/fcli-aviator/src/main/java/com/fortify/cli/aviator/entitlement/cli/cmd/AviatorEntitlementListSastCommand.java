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
package com.fortify.cli.aviator.entitlement.cli.cmd;

import com.fortify.cli.aviator._common.output.cli.mixin.AviatorOutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = AviatorOutputHelperMixins.ListSast.CMD_NAME)
public class AviatorEntitlementListSastCommand extends AbstractAviatorSastEntitlementListCommand {
    @Getter @Mixin private AviatorOutputHelperMixins.ListSast outputHelper;
}
