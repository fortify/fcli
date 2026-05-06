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
 * Enum values for the {@link FcliModuleCategory} annotation. Import individual
 * constants statically to get clean annotation syntax, e.g.:
 * <pre>
 *   import static com.fortify.cli.common.cli.util.FcliModuleCategories.PRODUCT;
 *   &#64;FcliModuleCategory(PRODUCT)
 * </pre>
 */
public enum FcliModuleCategories {
    /** Module interacts with a Fortify product (SSC, FoD, ScanCentral, Aviator, ...). */
    PRODUCT,
    /** Module manages fcli-level configuration (proxy, trust-store, ...). */
    CONFIG,
    /** Utility module (license, tool management, general utilities, ...). */
    UTIL
}
