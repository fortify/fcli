package com.fortify.cli.common.http.truststore.helper;

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
        return FcliHomeHelper.getFcliConfigPath().resolve("truststore.json");
    }
}
