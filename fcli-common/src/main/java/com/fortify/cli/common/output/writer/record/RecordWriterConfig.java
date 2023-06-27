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
package com.fortify.cli.common.output.writer.record;

import java.io.Writer;

import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.writer.IMessageResolver;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RecordWriterConfig {
    /** Writer to which to write the output */
    private Writer writer;
    /** Write singular output rather than an array/list; 
     * assumes that only a single record is passed to the {@link IRecordWriter} */ 
    private boolean singular;
    /** Free-format {@link IRecordWriter} options */
    private String options;
    /** The actual output format that was requested */
    private OutputFormat outputFormat;
    /** Whether to pretty-print the output */
    @Builder.Default private boolean pretty = true;
    /** I18n message resolver */
    private IMessageResolver messageResolver;
    /** Whether to add an {@value IActionCommandResultSupplier#actionFieldName} column */
    private boolean addActionColumn;
}
