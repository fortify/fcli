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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Classifies an fcli module command as a product integration, a configuration
 * module, or a utility module. Applied on the top-level module {@code *Commands}
 * class (e.g. {@code SSCCommands}, {@code ConfigCommands}).
 *
 * <p>Import {@link FcliModuleCategories} constants statically for concise usage:
 * <pre>
 *   import static com.fortify.cli.common.cli.util.FcliModuleCategories.PRODUCT;
 *   &#64;FcliModuleCategory(PRODUCT)
 * </pre>
 *
 * <p>Serialized to {@code metadata.moduleCategory} in the command spec node
 * produced by {@link CommandSpecDescriptor}.
 */
@FcliCommandMetadata
@Retention(RUNTIME)
@Target(TYPE)
public @interface FcliModuleCategory {
    FcliModuleCategories value();
}
