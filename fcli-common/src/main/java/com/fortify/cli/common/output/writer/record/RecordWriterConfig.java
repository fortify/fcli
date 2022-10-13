/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.output.writer.record;

import java.io.PrintWriter;

import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.IMessageResolver;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RecordWriterConfig {
    /** PrintWriter to which to write the output */
    private PrintWriter printWriter;
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
    /** Command that is outputting the data */
    private Object cmd;
}
