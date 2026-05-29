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

import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;

/**
 * SPI for product-specific context providers (SSC, FoD, etc.).
 * Implementations configure REST request helpers and SpEL variables
 * for a given product and session name.
 */
public interface IActionProductContextProvider {
    String getProductName();
    void configureActionContext(ActionRunnerContextLocal ctx, String sessionName);
    void configureSpelContext(IConfigurableSpelEvaluator spelEvaluator, ActionRunnerContextLocal ctx, String sessionName);
}
