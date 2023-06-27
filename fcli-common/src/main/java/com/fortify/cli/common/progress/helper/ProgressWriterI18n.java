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

import com.fortify.cli.common.output.writer.IMessageResolver;

public final class ProgressWriterI18n extends AbstractProgressWriterWrapper implements IProgressWriterI18n {
    private final IMessageResolver messageResolver;
   
    public ProgressWriterI18n(ProgressWriterType factory, IMessageResolver messageResolver) {
        super(factory);
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
