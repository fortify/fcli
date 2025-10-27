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
package com.fortify.cli.common.variable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class VariableContentsWriter extends Writer{
    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private PrintWriter pw;
    
    
    public VariableContentsWriter(String filePath) throws FileNotFoundException{
        fos = new FileOutputStream(filePath);
        osw =  new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        pw = new PrintWriter(osw);
    }


    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        pw.write(cbuf, off, len);
        
    }


    @Override
    public void flush() throws IOException {
        pw.flush();
        
    }


    @Override
    public void close() throws IOException {
        pw.close();
        osw.close();
        fos.close();
    }
    
    

}
