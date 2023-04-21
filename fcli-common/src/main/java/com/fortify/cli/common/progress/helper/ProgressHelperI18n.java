package com.fortify.cli.common.progress.helper;

import com.fortify.cli.common.output.writer.IMessageResolver;

public final class ProgressHelperI18n extends AbstractProgressHelperWrapper implements IProgressHelperI18n {
    private final IMessageResolver messageResolver;
   
    public ProgressHelperI18n(IMessageResolver messageResolver, boolean noProgress) {
        super(noProgress);
        this.messageResolver = messageResolver;
    }
    
    public final void writeI18nProgress(String keySuffix, Object... args) {
        writeProgress(getMessageString(keySuffix), args);
    }
    
    public final void writeI18nWarning(String keySuffix, Object... args) {
        writeWarning(getMessageString(keySuffix), args);
    }
    
    private String getMessageString(String keySuffix) {
        String messageFormat = messageResolver.getMessageString(keySuffix);
        if ( messageFormat==null ) {
            throw new RuntimeException(String.format("No resource bundle entry found for entry; please file a bug mention this message: class: %s, keySuffix: %s", this.getClass().getName(), keySuffix));
        }
        return messageFormat;
    }
}
