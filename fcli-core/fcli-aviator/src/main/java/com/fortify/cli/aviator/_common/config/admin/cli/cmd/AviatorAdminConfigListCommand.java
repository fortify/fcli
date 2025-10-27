/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator._common.config.admin.cli.cmd;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionListCommand;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME, sortOptions = false)
public class AviatorAdminConfigListCommand extends AbstractSessionListCommand<AviatorAdminConfigDescriptor> {
    @Mixin @Getter private OutputHelperMixins.List outputHelper;
    @Getter private AviatorAdminConfigHelper configHelper = AviatorAdminConfigHelper.instance();

    @Override
    protected AviatorAdminConfigHelper getSessionHelper() {
        return configHelper;
    }
}