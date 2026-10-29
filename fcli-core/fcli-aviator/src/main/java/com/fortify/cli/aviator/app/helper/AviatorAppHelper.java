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
package com.fortify.cli.aviator.app.helper;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;

/**
 * Helper methods for resolving Aviator applications specified as either id or name.
 * Numeric values are always interpreted as an application id, as was the case before
 * names were supported; all other values are interpreted as an application name. There
 * is no fallback from id to name, so applications with a purely numeric name can only
 * be addressed through their id.
 */
public final class AviatorAppHelper {
    private AviatorAppHelper() {}

    public static boolean isNumeric(String value) {
        if (value == null || value.isEmpty() || value.length() > 18) { return false; }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') { return false; }
        }
        return true;
    }

    /** Numeric values are returned as-is without contacting the server; names are resolved through the server-side name lookup. */
    public static String resolveApplicationId(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String appNameOrId) {
        if (appNameOrId == null || appNameOrId.isBlank()) {
            throw new AviatorSimpleException("Application name or id must be specified");
        }
        if (isNumeric(appNameOrId)) { return appNameOrId; }
        String tenant = configDescriptor.getTenant();
        String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(configDescriptor, tenant, appNameOrId);
        return String.valueOf(client.getApplicationByName(appNameOrId, messageAndSignature[1], messageAndSignature[0], tenant).getId());
    }
}
