package com.fortify.cli.common.progress.helper;

import com.fortify.cli.common.output.writer.IMessageResolver;

import lombok.Setter;

public abstract class AbstractI18nProgressHelperWrapper extends AbstractProgressHelperWrapper {
    @Setter private IMessageResolver messageResolver;
    
    public final void writeI18nProgress(String keySuffix, Object... args) {
        String messageFormat = getMessageResolver().getMessageString(keySuffix);
        if ( messageFormat==null ) {
            throw new RuntimeException(String.format("No resource bundle entry found for entry; please file a bug mention this message: class: %s, keySuffix: %s", this.getClass().getName(), keySuffix));
        }
        writeProgress(messageFormat, args);
    }
    
    private final IMessageResolver getMessageResolver() {
        if ( messageResolver==null ) {
            setMessageResolver(createMessageResolver());
        }
        if ( messageResolver==null ) {
            throw new RuntimeException("Class must call setMessageResolver() or override createMessageResolver; please file a bug mentioning this message: "+this.getClass().getName());
        }
        return messageResolver;
    }
    
    protected IMessageResolver createMessageResolver() { return null; }
}
