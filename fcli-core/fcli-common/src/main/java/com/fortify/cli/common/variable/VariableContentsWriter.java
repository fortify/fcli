package com.fortify.cli.common.variable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;

public class VariableContentsWriter extends Writer{
    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private PrintWriter pw;
    
    
    @SneakyThrows
    public VariableContentsWriter(String filePath) {
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
