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
package com.fortify.cli.common.http.ssl.truststore.helper;

import java.nio.file.Path;

import com.fortify.cli.common.http.ssl.trust.FcliTrustManager;
import com.fortify.cli.common.util.FcliDataHelper;

public final class TrustStoreConfigHelper {
    private TrustStoreConfigHelper() {}
    
    public static final TrustStoreConfigDescriptor getTrustStoreConfig() {
        Path trustStoreConfigPath = getTrustStoreConfigPath();
        return !FcliDataHelper.exists(trustStoreConfigPath) 
                ? new TrustStoreConfigDescriptor() 
                : FcliDataHelper.readSecuredFile(trustStoreConfigPath, TrustStoreConfigDescriptor.class, true);
    }
    
    public static final TrustStoreConfigDescriptor setTrustStoreConfig(TrustStoreConfigDescriptor descriptor) {
        Path trustStoreConfigPath = getTrustStoreConfigPath();
        FcliDataHelper.saveSecuredFile(trustStoreConfigPath, descriptor, true);
        // Refresh trust manager for RPC/MCP servers.
        FcliTrustManager.refresh();
        return descriptor;
    }
    
    public static final void clearTrustStoreConfig() {
        FcliDataHelper.deleteFile(getTrustStoreConfigPath(), true);
        // Refresh trust manager for RPC/MCP servers.
        FcliTrustManager.refresh();
    }
    
    private static final Path getTrustStoreConfigPath() {
        return FcliDataHelper.getFcliConfigPath().resolve("ssl/truststore.json");
    }
}
