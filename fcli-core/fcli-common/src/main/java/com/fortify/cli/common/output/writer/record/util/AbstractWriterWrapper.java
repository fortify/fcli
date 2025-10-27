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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This abstract class wraps a Writer instance, delegating all method calls to
 * the wrappee. This class is intended as a base class for wrappers that override
 * one or more writer characteristics.
 */
@RequiredArgsConstructor
public abstract class AbstractWriterWrapper<T extends Writer> extends Writer {
    @Getter private final T wrappee;

    @Override
    public void close() throws IOException {
        wrappee.close();
    }

    @Override
    public void flush() throws IOException {
        wrappee.flush();
    }

    @Override
    public void write(char[] arg0, int arg1, int arg2) throws IOException {
        wrappee.write(arg0, arg1, arg2);
    }
}
