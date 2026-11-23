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
 * Values in canonical numeric form (digits without leading zeros) are interpreted as an
 * application id, as was the case before names were supported; all other values, including
 * numeric values with leading zeros like {@code 000144}, are interpreted as an application
 * name and passed to the server exactly as specified. There is no fallback from id to name,
 * so applications whose name is a canonical number can only be addressed through their id.
 */
public final class AviatorAppHelper {
    private AviatorAppHelper() {}

    public static boolean isCanonicalId(String value) {
        if (value == null || value.isEmpty() || value.length() > 18) { return false; }
        if (value.length() > 1 && value.charAt(0) == '0') { return false; }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') { return false; }
        }
        return true;
    }

    /**
     * Canonical numeric ids are returned as-is without contacting the server (the server rejects ids that don't
     * match their canonical form); any other value is treated as a name, resolved through the server-side name lookup.
     */
    public static String resolveApplicationId(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String appNameOrId) {
        if (appNameOrId == null || appNameOrId.isBlank()) {
            throw new AviatorSimpleException("Application name or id must be specified");
        }
        if (isCanonicalId(appNameOrId)) { return appNameOrId; }
        String tenant = configDescriptor.getTenant();
        String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(configDescriptor, tenant, appNameOrId);
        return String.valueOf(client.getApplicationByName(appNameOrId, messageAndSignature[1], messageAndSignature[0], tenant).getId());
    }
}
