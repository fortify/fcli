/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion.helper;

import javax.validation.ValidationException;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

@Data @ReflectiveAccess
public final class SSCAppAndVersionNameDescriptor {
    private final String appName, versionName;
    
    public static final SSCAppAndVersionNameDescriptor fromCombinedAppAndVersionName(String appAndVersionName, String delimiter) {
        String[] appAndVersionNameArray = appAndVersionName.split(delimiter);
        if ( appAndVersionNameArray.length != 2 ) { 
            throw new ValidationException("Application and version name must be specified in the format <application name>"+delimiter+"<version name>"); 
        }
        return new SSCAppAndVersionNameDescriptor(appAndVersionNameArray[0], appAndVersionNameArray[1]);
    }
}