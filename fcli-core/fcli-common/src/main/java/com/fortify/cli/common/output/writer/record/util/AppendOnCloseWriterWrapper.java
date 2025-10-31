/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.output.writer.record.util;

import java.io.IOException;
import java.io.Writer;

/**
 * This class wraps a Writer instance, delegating all method calls to the
 * wrappee. Before closing the wrappee, the string specified in the constructor 
 * will be appended to the wrappee.
 */
public class AppendOnCloseWriterWrapper extends AbstractWriterWrapper<Writer> {
    private final String stringToAppend;
    public AppendOnCloseWriterWrapper(String stringToAppend, Writer wrappee) {
        super(wrappee);
        this.stringToAppend = stringToAppend;
    }
    
    @Override
    public void close() throws IOException {
        getWrappee().append(stringToAppend);
        getWrappee().flush();
        super.close();
    }
}
