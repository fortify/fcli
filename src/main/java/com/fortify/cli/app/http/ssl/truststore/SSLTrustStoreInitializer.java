/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.app.http.ssl.truststore;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;
import com.fortify.cli.common.util.StringUtils;

import jakarta.inject.Singleton;

/**
 * This class is responsible for setting up the Java SSL trust store configuration.
 * 
 * @author Ruud Senden
 */
@Singleton
public class SSLTrustStoreInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
    	TrustStoreConfigDescriptor descriptor = TrustStoreConfigHelper.getTrustStoreConfig();
    	if ( descriptor!=null && StringUtils.isNotBlank(descriptor.getPath()) ) {
    		Path absolutePath = Path.of(descriptor.getPath()).toAbsolutePath();
        	if ( !Files.exists(absolutePath) ) {
        		throw new IllegalArgumentException("Trust store cannot be found: "+absolutePath);
        	}
    		System.setProperty("javax.net.ssl.trustStore", descriptor.getPath());
    		if ( StringUtils.isNotBlank(descriptor.getType()) ) {
    			System.setProperty("javax.net.ssl.trustStoreType", descriptor.getType());
    		}
    		if ( StringUtils.isNotBlank(descriptor.getPassword()) ) {
    			System.setProperty("javax.net.ssl.trustStorePassword", descriptor.getPassword());
    		}
    	}
    }
}
