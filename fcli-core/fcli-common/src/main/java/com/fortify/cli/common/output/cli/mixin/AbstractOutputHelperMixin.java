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
package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;

public abstract class AbstractOutputHelperMixin implements IOutputHelper {
    @Override
    public void write(IObjectNodeProducer objectNodeProducer) {
        getOutputWriterFactory().createOutputWriter(getBasicOutputConfig()).write(objectNodeProducer);
    }

    /** Indicates whether selected output format supports streaming (table returns false). */
    public boolean isStreamingOutputSupported() {
        if ( getOutputWriterFactory() instanceof StandardOutputWriterFactoryMixin sowfm ) {
            var selected = sowfm.getSelectedRecordWriterFactory(getBasicOutputConfig());
            return selected.isStreaming();
        }
        return getBasicOutputConfig().defaultFormat().isStreaming();
    }

    protected abstract StandardOutputConfig getBasicOutputConfig();

    protected abstract IOutputWriterFactory getOutputWriterFactory();

    public RecordWriterStyle getRecordWriterStyle() {
        return getOutputWriterFactory().getRecordWriterStyle();
    }
}
