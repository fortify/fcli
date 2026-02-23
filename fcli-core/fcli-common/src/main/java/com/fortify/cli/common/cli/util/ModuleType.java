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
package com.fortify.cli.common.cli.util;

/**
 * Enum that holds the type of a module as a product module (SSC, FoD, Aviator, SC-SAST, SC-DAST)
 * or a non-product/other module (util, tool, license, actions, config, ...)
 * 
 * @author Sangamesh Vijaykumar
 */

public enum ModuleType {
    PRODUCT,   // SSC, FoD, Aviator, SC-SAST, SC-DAST
    OTHER      // util, tool, license, actions, config, ...
}
