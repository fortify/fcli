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
package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.json.record.IRecordProducer;

public interface IOutputWriter {
    /**
     * Write records provided by the given {@link IRecordProducer} to the configured output(s).
     */
    void write(IRecordProducer recordProducer);

}