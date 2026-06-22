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
package com.fortify.cli.common.action.runner;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.action.model.FcliActionValidationException;

/**
 * Static registry for product context providers. Providers are registered
 * at startup by the application initializer and looked up by product name
 * when processing {@code with.product} steps.
 */
public final class ActionProductContextProviders {
    private static final Map<String, IActionProductContextProvider> providers = new LinkedHashMap<>();

    private ActionProductContextProviders() {}

    public static void register(IActionProductContextProvider provider) {
        providers.put(provider.getProductName(), provider);
    }

    public static IActionProductContextProvider get(String product) {
        var provider = providers.get(product);
        if (provider == null) {
            throw new FcliActionValidationException("Unknown product: " + product
                    + ". Available products: " + providers.keySet());
        }
        return provider;
    }
}
