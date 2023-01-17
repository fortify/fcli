/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.app.truststore;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;
import com.fortify.cli.common.http.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.util.StringUtils;

import jakarta.inject.Singleton;

/**
 * This class is responsible for setting up the Java SSL trust store configuration.
 * 
 * @author Ruud Senden
 */
@Singleton
public class TrustStoreInitializer implements IFortifyCLIInitializer {
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
