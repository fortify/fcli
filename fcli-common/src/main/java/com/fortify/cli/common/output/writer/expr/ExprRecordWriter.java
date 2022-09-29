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
package com.fortify.cli.common.output.writer.expr;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.IRecordWriter;
import com.fortify.cli.common.output.writer.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class ExprRecordWriter implements IRecordWriter {
    private static final Pattern exprPattern = Pattern.compile("\\{(.+?)\\}");
    @Getter private final RecordWriterConfig config;
    
    private PrintWriter getPrintWriter() {
        return getConfig().getPrintWriter();
    }

    @Override @SneakyThrows
    public void writeRecord(ObjectNode record) {
        getPrintWriter().print(evaluateExpression(config.getOptions(), record));
    }

    private static final String evaluateExpression(String expr, ObjectNode input) {
        expr = insertControlCharacters(expr);
        StringBuilder sb = new StringBuilder();
        Matcher matcher = exprPattern.matcher(expr);
        while (matcher.find()) {
            String propertyPath = matcher.group(1);
            String value = JsonHelper.evaluateJsonPath(input, propertyPath, String.class);
            if ( value==null ) { value = matcher.group(2); }
            if ( value==null ) { value = ""; }
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static final String insertControlCharacters(String s) {
        return s.replaceAll("\\\\t", "\t")
                .replaceAll("\\\\b", "\b")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\\\f", "\f");
    }
    
    
}
