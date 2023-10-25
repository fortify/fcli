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
package com.fortify.cli.common.output.writer;

import com.fortify.cli.common.util.PicocliSpecHelper;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;

@RequiredArgsConstructor
public final class CommandSpecMessageResolver implements IMessageResolver {
    private final CommandSpec commandSpec;
    
    @Override
    public String getMessageString(String keySuffix) {
        return PicocliSpecHelper.getMessageString(commandSpec, keySuffix);
    }
}
