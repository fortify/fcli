/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.progress.helper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractProgressWriterWrapper implements IProgressWriter {
    private final ProgressWriterType factory;
    private IProgressWriter progressWriter;
    
    @Override
    public final boolean isMultiLineSupported() {
        return getProgressWriter().isMultiLineSupported();
    }
    
    @Override
    public final void writeProgress(String message, Object... args) {
        getProgressWriter().writeProgress(message, args);
    }
    
    @Override
    public final void writeWarning(String message, Object... args) {
        getProgressWriter().writeWarning(message, args);
    }
    
    @Override
    public final void clearProgress() {
        getProgressWriter().clearProgress();
    }
    
    @Override
    public void close() {
        getProgressWriter().close();
    }
    
    private final IProgressWriter getProgressWriter() {
        if ( progressWriter==null ) {
            progressWriter = createProgressWriter();
        }
        return progressWriter;
    }

    private final IProgressWriter createProgressWriter() {
        return factory.create();
    }
}
