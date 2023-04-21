package com.fortify.cli.common.progress.helper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractProgressHelperWrapper implements IProgressHelper {
    private final boolean noProgress;
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
    public final void writeWarning(String message, Object... args) {
        getProgressHelper().writeWarning(message, args);
    }
    
    @Override
    public final void clearProgress() {
        getProgressHelper().clearProgress();
    }
    
    @Override
    public void close() {
        getProgressHelper().close();
    }
    
    private final IProgressHelper getProgressHelper() {
        if ( progressHelper==null ) {
            progressHelper = createProgressHelper();
        }
        return progressHelper;
    }

    private final IProgressHelper createProgressHelper() {
        return ProgressHelperFactory.createProgressHelper(noProgress);
    }
}
