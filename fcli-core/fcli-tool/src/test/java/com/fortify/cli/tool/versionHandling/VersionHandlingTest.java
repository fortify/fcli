package com.fortify.cli.tool.versionHandling;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.tool._common.helper.ToolDownloadDescriptor;
import com.fortify.cli.tool._common.helper.ToolHelper;

import org.junit.jupiter.api.Assertions;


public class VersionHandlingTest {
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    String toolName = "fod-uploader";
    
    @Test
    public void testVersionHandling() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceFile = ToolHelper.getResourceFile(toolName, String.format("%s.yaml", toolName));
        try (InputStream file = classLoader.getResourceAsStream(resourceFile)) {

            var downloadDescriptor = yamlObjectMapper.readValue(file, ToolDownloadDescriptor.class); 
            var d = downloadDescriptor.getVersion("5");
            //var d2 = downloadDescriptor.getVersion("20.");
            var d3 = downloadDescriptor.getVersion("5.0");
            var d4 = downloadDescriptor.getVersion("5.0.0");
            
            Assertions.assertEquals(d.getVersion(), "5.4.0");
            //Assertions.assertEquals(d2.getVersion(), "20.2.4");
            Assertions.assertEquals(d3.getVersion(), "5.0.1");
            Assertions.assertEquals(d4.getVersion(), "5.0.0");
        } catch(Exception e) {
            throw e;
        }
    }
}

