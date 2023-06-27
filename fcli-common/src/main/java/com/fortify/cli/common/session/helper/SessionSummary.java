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
package com.fortify.cli.common.session.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @ReflectiveAccess @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionSummary {
    public static final Date EXPIRES_UNKNOWN = null;
    public static final Date EXPIRES_NEVER = new Date(Long.MAX_VALUE);
    private String name;
    private String type;
    private String url;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss z") 
    private Date created;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss z") 
    private Date expires;
    
    public String getExpired() {
        if ( expires==null ) { 
            return "Unknown";
        } else if ( expires.after(new Date()) ) {
            return "No";
        } else {
            return "Yes";
        }
    }
}
