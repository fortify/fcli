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
package com.fortify.cli.aviator._common.session.user.cli.cmd;

import java.util.Date;

import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionLoginOptions;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionNameArgGroup;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserTokenResolverMixin;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator._common.util.AviatorJwtUtils;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class AviatorUserSessionLoginCommand extends AbstractSessionLoginCommand<AviatorUserSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private AviatorUserSessionHelper sessionHelper = AviatorUserSessionHelper.instance();
    @Mixin @Getter private AviatorUserTokenResolverMixin tokenResolver;
    @Mixin private AviatorUserSessionLoginOptions sessionLoginOptions = new AviatorUserSessionLoginOptions();
    @Getter @ArgGroup(headingKey = "aviator.user-session.name.arggroup")
    private AviatorUserSessionNameArgGroup sessionNameSupplier;

    @Override
    protected void logoutBeforeNewLogin(String sessionName, AviatorUserSessionDescriptor sessionDescriptor) {}

    @Override
    protected AviatorUserSessionDescriptor login(String sessionName) {
        String resolvedToken = tokenResolver.getToken();
        Date expiryDate = AviatorJwtUtils.extractExpiryDateFromToken(resolvedToken);

        return AviatorUserSessionDescriptor.builder()
                .aviatorUrl(sessionLoginOptions.getAviatorUrl())
                .aviatorToken(resolvedToken)
                .expiryDate(expiryDate)
                .build();
    }
}