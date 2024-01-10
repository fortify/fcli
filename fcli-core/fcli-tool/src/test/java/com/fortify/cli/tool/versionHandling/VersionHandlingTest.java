package com.fortify.cli.tool.versionHandling;

import org.junit.jupiter.api.Test;

import com.fortify.cli.tool._common.helper.ToolHelper;

import org.junit.jupiter.api.Assertions;


public class VersionHandlingTest {
    String toolName = "fod-uploader";
    
    @Test
    public void testVersionHandling() throws Exception {
        
        var downloadDescriptor = ToolHelper.getToolDownloadDescriptor(toolName);; 
        var d = downloadDescriptor.getVersion("5");
        //var d2 = downloadDescriptor.getVersion("20.");
        var d3 = downloadDescriptor.getVersion("5.0");
        var d4 = downloadDescriptor.getVersion("5.0.0");
        
        Assertions.assertEquals(d.getVersion(), "5.4.0");
        //Assertions.assertEquals(d2.getVersion(), "20.2.4");
        Assertions.assertEquals(d3.getVersion(), "5.0.1");
        Assertions.assertEquals(d4.getVersion(), "5.0.0");
    }
}

