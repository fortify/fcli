/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod.action.helper;

import com.fortify.cli.common.action.runner.n.ActionParameterHelper.ParameterValueSupplier;
import com.fortify.cli.common.action.runner.n.ActionRuntimeConfig;

public final class FoDActionHelper {
    public static final ActionRuntimeConfig createActionRuntimeConfig() {
        return ActionRuntimeConfig.builder()
                .actionRunCommand("fcli fod action run")
                .optionSpecTypeConfigurer("release_single", (b,p)->ParameterValueSupplier.configure(b, null))
                .build();
    }
}
