package com.fortify.cli.common.output.writer.record;

import com.fortify.cli.common.util.StringUtils;

import lombok.Getter;

public abstract class AbstractRecordWriter implements IRecordWriter {
    @Getter private final RecordWriterConfig config;
    
    public AbstractRecordWriter(RecordWriterConfig config) {
        this.config = updateConfig(config);;
    }

    protected RecordWriterConfig updateConfig(RecordWriterConfig config) {
        String options = config.getOptions();
        if ( StringUtils.isBlank(options) ) {
            String keySuffix = "output."+config.getOutputFormat().getMessageKey()+".options";
            options = config.getMessageResolver().getMessageString(keySuffix);
        }
        config.setOptions(options);
        return config;
    }
    
    
}
