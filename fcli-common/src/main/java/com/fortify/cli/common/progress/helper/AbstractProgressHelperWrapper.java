package com.fortify.cli.common.progress.helper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractProgressHelperWrapper implements IProgressHelper {
    private IProgressHelper progressHelper;
    
    @Override
    public final boolean isMultiLineSupported() {
        return getProgressHelper().isMultiLineSupported();
    }
    
    @Override
    public final void writeProgress(String message, Object... args) {
        getProgressHelper().writeProgress(message, args);
    }
    
    @Override
    public final void clearProgress() {
        getProgressHelper().clearProgress();
    }
    
    private final IProgressHelper getProgressHelper() {
        if ( progressHelper==null ) {
            progressHelper = createProgressHelper();
        }
        return progressHelper;
    }

    protected abstract IProgressHelper createProgressHelper();
}
