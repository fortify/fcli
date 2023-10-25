/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
