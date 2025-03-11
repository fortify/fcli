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
package com.fortify.cli.ssc._common.session.cli.cmd;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.rest.cli.mixin.SSCAndScanCentralUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.rest.helper.SSCAndScanCentralUnirestHelper;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCAndScanCentralSessionLoginOptions;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCSessionNameArgGroup;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralUrlConfig;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class SSCSessionLoginCommand extends AbstractSessionLoginCommand<SSCAndScanCentralSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private SSCAndScanCentralSessionHelper sessionHelper = SSCAndScanCentralSessionHelper.instance();
    @Mixin private SSCAndScanCentralSessionLoginOptions sessionLoginOptions;
    @Getter @ArgGroup(headingKey = "ssc.session.name.arggroup") 
    private SSCSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SSCAndScanCentralSessionDescriptor sessionDescriptor) {
        SSCAndScanCentralUnirestInstanceSupplierMixin.shutdownUnirestInstance(sessionName);
        sessionDescriptor.logout(sessionLoginOptions.getSscAndScanCentralCredentialConfigOptions().getSscUserCredentialsConfig());
    }
    
    @Override
    protected SSCAndScanCentralSessionDescriptor login(String sessionName) {
        ISSCAndScanCentralUrlConfig urlConfig = sessionLoginOptions.getSscAndScanCentralUrlConfigOptions();
        ISSCAndScanCentralCredentialsConfig credentialsConfig = sessionLoginOptions.getSscAndScanCentralCredentialConfigOptions();
        return SSCAndScanCentralSessionDescriptor.create(urlConfig, credentialsConfig);
    }
    
    @Override
    protected void testAuthenticatedConnection(String sessionName) {
        // SSC connection will already have been validated during login, so we only need to
        // verify SC-SAST & SC-DAST connections.
        SSCAndScanCentralSessionDescriptor sessionData = SSCAndScanCentralSessionHelper.instance().get(sessionName, true);
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            testAuthenticatedSCSastConnection(unirest, sessionData);
        }
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            testAuthenticatedSCDastConnection(unirest, sessionData);
        }
    }

    private void testAuthenticatedSCSastConnection(UnirestInstance u, SSCAndScanCentralSessionDescriptor sessionData) {
        if ( StringUtils.isBlank(sessionData.getScSastDisabledReason()) ) {
            SSCAndScanCentralUnirestHelper.configureScSastControllerUnirestInstance(u, sessionData);
            try {
                u.get("/rest/v2/ping").asString().getBody();
            } catch ( UnexpectedHttpResponseException e ) {
                if ( e.getStatus()==401 ) {
                    throw new FcliSimpleException("Error authenticating with SC SAST Controller; please check that --client-auth-token option value matches the client_auth_token as configured in config.properties on SC SAST Controller", e);
                } else {
                    throw new FcliSimpleException("Error connecting to SC SAST Controller; please check URL configuration", e);
                }
            }
        }
    }
    
    private void testAuthenticatedSCDastConnection(UnirestInstance u, SSCAndScanCentralSessionDescriptor sessionData) {
        if ( StringUtils.isBlank(sessionData.getScDastDisabledReason()) ) {
            SSCAndScanCentralUnirestHelper.configureScDastControllerUnirestInstance(u, sessionData);
            try {
                u.get("/api/v2/utilities/about").asString().getBody();
            } catch ( UnexpectedHttpResponseException e ) {
                throw new FcliSimpleException("Error connecting to SC DAST API; please check URL configuration", e);
            }
        }
    }
}
