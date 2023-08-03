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

package com.fortify.cli.fod.microservice.helper;

import com.fortify.cli.fod.release.helper.FoDQualifiedReleaseNameDescriptor;

import lombok.Data;

@Data
public final class FoDQualifiedMicroserviceNameDescriptor {
    private final String appName, microserviceName;
    
    public static final FoDQualifiedMicroserviceNameDescriptor fromCombinedAppAndMicroserviceName(String appAndMicroserviceName, String delimiter) {
        String[] appAndMicroserviceNameArray = appAndMicroserviceName.split(delimiter);
        if (appAndMicroserviceNameArray.length != 2) {
            throw new IllegalArgumentException("Application and microservice name must be specified in the format <application name>"+delimiter+"<microservice name>");
        }
        return new FoDQualifiedMicroserviceNameDescriptor(appAndMicroserviceNameArray[0], appAndMicroserviceNameArray[1]);
    }

    public static FoDQualifiedMicroserviceNameDescriptor from(FoDQualifiedReleaseNameDescriptor desc) {
        return new FoDQualifiedMicroserviceNameDescriptor(desc.getAppName(), desc.getMicroserviceName());
    }
}