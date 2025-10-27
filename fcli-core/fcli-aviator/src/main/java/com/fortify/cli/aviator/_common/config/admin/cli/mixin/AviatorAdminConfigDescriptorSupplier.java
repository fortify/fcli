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
package com.fortify.cli.aviator._common.config.admin.cli.mixin;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigHelper;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;

public class AviatorAdminConfigDescriptorSupplier extends AbstractSessionDescriptorSupplierMixin<AviatorAdminConfigDescriptor> {
    @Getter @ArgGroup(headingKey = "aviator.admin-config.name.arggroup")
    private AviatorAdminConfigNameArgGroup configNameSupplier;

    @Override
    protected final AviatorAdminConfigDescriptor getSessionDescriptor(String configName) {
        return AviatorAdminConfigHelper.instance().get(configName, true);
    }

    @Override
    public ISessionNameSupplier getSessionNameSupplier() {
        return configNameSupplier;
    }

    public final AviatorAdminConfigDescriptor getConfigDescriptor() {
        return getSessionDescriptor();
    }
}