/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.app.i18n;

import java.util.Locale;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;
import com.fortify.cli.common.cli.util.IFortifyCLIInitializer;
import com.fortify.cli.common.i18n.helper.LanguageHelper;

import jakarta.inject.Singleton;

@Singleton
public class LanguageInitializer implements IFortifyCLIInitializer {
    @Override
    public void initializeFortifyCLI(FortifyCLIInitializerCommand cmd) {
        Locale.setDefault(LanguageHelper.getConfiguredLanguageDescriptor().getLocale());
    }
}