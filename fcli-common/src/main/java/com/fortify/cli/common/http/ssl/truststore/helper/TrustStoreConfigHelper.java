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
package com.fortify.cli.common.http.ssl.truststore.helper;

import java.nio.file.Path;

import com.fortify.cli.common.util.FcliHomeHelper;

public final class TrustStoreConfigHelper {
    private TrustStoreConfigHelper() {}
    
    public static final TrustStoreConfigDescriptor getTrustStoreConfig() {
        Path trustStoreConfigPath = getTrustStoreConfigPath();
        return !FcliHomeHelper.exists(trustStoreConfigPath) 
        		? new TrustStoreConfigDescriptor() 
        		: FcliHomeHelper.readSecuredFile(trustStoreConfigPath, TrustStoreConfigDescriptor.class, true);
    }
    
    public static final TrustStoreConfigDescriptor setTrustStoreConfig(TrustStoreConfigDescriptor descriptor) {
        Path trustStoreConfigPath = getTrustStoreConfigPath();
        FcliHomeHelper.saveSecuredFile(trustStoreConfigPath, descriptor, true);
        return descriptor;
    }
    
    public static final void clearTrustStoreConfig() {
        FcliHomeHelper.deleteFile(getTrustStoreConfigPath(), true);
    }
    
    private static final Path getTrustStoreConfigPath() {
        return FcliHomeHelper.getFcliConfigPath().resolve("ssl/truststore.json");
    }
}
