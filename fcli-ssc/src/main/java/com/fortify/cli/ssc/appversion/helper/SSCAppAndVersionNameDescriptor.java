package com.fortify.cli.ssc.appversion.helper;

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