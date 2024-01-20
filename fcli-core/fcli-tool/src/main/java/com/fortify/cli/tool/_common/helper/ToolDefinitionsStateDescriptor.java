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
package com.fortify.cli.tool._common.helper;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data
public final class ToolDefinitionsStateDescriptor{
    private String source;
    private Date lastUpdate;
    private String __action__;
    
    public final String getLastUpdateString() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(getLastUpdate());
    }
}