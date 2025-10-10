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
package com.fortify.cli.debricked._common.session.cli.cmd;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.debricked._common.session.cli.mixin.DebrickedSessionNameArgGroup;
import com.fortify.cli.debricked._common.session.cli.mixin.DebrickedShortLoginOptions;
import com.fortify.cli.debricked._common.session.cli.mixin.DebrickedUnirestInstanceSupplierMixin;
import com.fortify.cli.debricked._common.session.helper.DebrickedAuthHelper;
import com.fortify.cli.debricked._common.session.helper.DebrickedJwtTokenResponse;
import com.fortify.cli.debricked._common.session.helper.DebrickedSessionDescriptor;
import com.fortify.cli.debricked._common.session.helper.DebrickedSessionHelper;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class DebrickedSessionLoginCommand extends AbstractSessionLoginCommand<DebrickedSessionDescriptor> {
    @Getter @Mixin private OutputHelperMixins.Login outputHelper;
    @Getter private DebrickedSessionHelper sessionHelper = DebrickedSessionHelper.instance();
    @Mixin private DebrickedShortLoginOptions loginOptions;
    @Getter @ArgGroup(headingKey = "debricked.session.name.arggroup") 
    private DebrickedSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, DebrickedSessionDescriptor sessionDescriptor) {
        DebrickedUnirestInstanceSupplierMixin.shutdownUnirestInstance(sessionName);
    }

    @Override
    protected DebrickedSessionDescriptor login(String sessionName) {
        DebrickedSessionDescriptor sessionDescriptor;
        
        try (var unirest = GenericUnirestFactory.createUnirestInstance()) {
            // Create a temporary DebrickedHelper to get JWT token
            DebrickedAuthHelper.configureNonAuthenticatedUnirest(unirest, loginOptions.getUrlConfigOptions());
            // TODO Do we also want to store refreshToken? (in which case we should call getJwtTokenResponse instead)
            DebrickedJwtTokenResponse jwtTokenResponse = DebrickedAuthHelper.getJwtTokenResponse(unirest, loginOptions);
            sessionDescriptor = new DebrickedSessionDescriptor(
                loginOptions.getUrlConfigOptions(), 
                jwtTokenResponse.getToken(),
                jwtTokenResponse.getRefreshToken()
            );
        } catch (Exception e) {
            throw new FcliSimpleException("Unable to authenticate with Debricked", e);
        }
        
        return sessionDescriptor;
    }
}