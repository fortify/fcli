/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.tool.definitions.helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;

@Reflectable // We only serialize, not deserialize, so no no-args contructor
@Data
public final class ToolDefinitionsOutputDescriptor {
    private final String name;
    private final String source;
    private final String lastUpdate;
    
    public ToolDefinitionsOutputDescriptor(String name, ToolDefinitionsStateDescriptor stateDescriptor) {
        this(name, stateDescriptor.getSource(), stateDescriptor.getLastUpdate());
    }
    
    public ToolDefinitionsOutputDescriptor(String name, String source, Date lastUpdate) {
        this.name = name;
        this.source = getFormattedString(source);
        this.lastUpdate = lastUpdate==null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(lastUpdate);
    }
    
    private static final String getFormattedString(String str) {
        List<String> parts = new ArrayList<>();
        int size = 29, length = str.length();
        for(int i = 0, end, goodPos; i < length; i = end) {
            end = Math.min(length, i + size);
            goodPos = str.lastIndexOf('/', end);
            if(goodPos <= i) goodPos = end; else end = goodPos + 1;
            parts.add(str.substring(i, goodPos));
        }
        return String.join("\n/", parts);
    }
}