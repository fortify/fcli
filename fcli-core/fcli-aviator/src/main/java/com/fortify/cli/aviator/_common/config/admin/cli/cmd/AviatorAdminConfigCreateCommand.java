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
package com.fortify.cli.aviator._common.config.admin.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.config.admin.cli.mixin.AviatorAdminConfigCreateOptions;
import com.fortify.cli.aviator._common.config.admin.cli.mixin.AviatorAdminConfigNameArgGroup;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Create.CMD_NAME, sortOptions = false)
public class AviatorAdminConfigCreateCommand extends AbstractSessionLoginCommand<AviatorAdminConfigDescriptor> {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAdminConfigCreateCommand.class);

    @Mixin @Getter private OutputHelperMixins.Create outputHelper;
    @Getter private AviatorAdminConfigHelper configHelper = AviatorAdminConfigHelper.instance();
    @Mixin private AviatorAdminConfigCreateOptions configCreateOptions = new AviatorAdminConfigCreateOptions();
    @Getter @ArgGroup(headingKey = "aviator.admin-config.name.arggroup")
    private AviatorAdminConfigNameArgGroup configNameSupplier;

    @Override
    public ISessionNameSupplier getSessionNameSupplier() {
        return configNameSupplier;
    }

    @Override
    protected void logoutBeforeNewLogin(String configName, AviatorAdminConfigDescriptor configDescriptor) {
    }

    @Override
    protected AviatorAdminConfigDescriptor login(String configName) {
        String privateKeyContents = configCreateOptions.getPrivateKeyResolver().getPrivateKeyContents();
        AviatorAdminConfigDescriptor configDescriptor = new AviatorAdminConfigDescriptor(
                configCreateOptions.getAviatorUrl(),
                configCreateOptions.getTenant(),
                privateKeyContents);
        LOG.info("Validating Aviator admin configuration with the Aviator server...");
        configHelper.validateConfig(configDescriptor);
        LOG.info("Aviator admin configuration validated successfully.");
        return configDescriptor;
    }

    @Override
    protected AviatorAdminConfigHelper getSessionHelper() {
        return configHelper;
    }
}
