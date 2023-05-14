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
