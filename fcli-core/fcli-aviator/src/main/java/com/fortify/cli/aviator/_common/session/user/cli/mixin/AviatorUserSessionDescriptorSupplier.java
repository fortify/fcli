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
package com.fortify.cli.aviator._common.session.user.cli.mixin;

import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;

public class AviatorUserSessionDescriptorSupplier extends AbstractSessionDescriptorSupplierMixin<AviatorUserSessionDescriptor> {
    @Getter @ArgGroup(headingKey = "aviator.user-session.name.arggroup") 
    private AviatorUserSessionNameArgGroup sessionNameSupplier;
    
    @Override
    public final AviatorUserSessionDescriptor getSessionDescriptor(String sessionName) {
        return AviatorUserSessionHelper.instance().get(sessionName, true);
    }
}
