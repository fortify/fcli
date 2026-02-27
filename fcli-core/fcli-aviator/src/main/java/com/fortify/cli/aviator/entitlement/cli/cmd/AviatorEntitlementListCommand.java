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

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Deprecated entitlement list command retained for backward compatibility.
 * Use {@code fcli aviator entitlement list-sast} instead.
 */
@Command(name = OutputHelperMixins.List.CMD_NAME)
public class AviatorEntitlementListCommand extends AbstractAviatorSastEntitlementListCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
}