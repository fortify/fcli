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
package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.common.rest.wait.StandardWaitHelperProgressMonitor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command
public class StandardWaitHelperProgressMonitorMixin {
    @Mixin private CommandHelperMixin commandHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    public StandardWaitHelperProgressMonitor create(boolean writeFinalStatus) {
        return progressWriterFactory.getType()==ProgressWriterType.none 
                ? null 
                : new StandardWaitHelperProgressMonitor(progressWriterFactory.create(), commandHelper.getMessageResolver(), writeFinalStatus);
    }
}
