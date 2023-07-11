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

package com.fortify.cli.fod.entity.release.cli.mixin;

import javax.validation.ValidationException;

import lombok.Data;

@Data
public final class FoDAppMicroserviceAndRelNameDescriptor {
    private final String appName, microserviceName, relName;
    
    public static final FoDAppMicroserviceAndRelNameDescriptor fromCombinedAppMicroserviceAndRelName(String appMicroserviceAndRelName, String delimiter) {
        String[] appMicroserviceAndRelNameArray = appMicroserviceAndRelName.split(delimiter);
        if (appMicroserviceAndRelNameArray.length < 2) {
            throw new ValidationException("Application microservice and release name must be specified in the format <application name>"+delimiter+"<microservice name>"+delimiter+"<release name>");
        }
        if (appMicroserviceAndRelNameArray.length == 3) {
            return new FoDAppMicroserviceAndRelNameDescriptor(appMicroserviceAndRelNameArray[0], appMicroserviceAndRelNameArray[1], appMicroserviceAndRelNameArray[2]);
        } else {
            return new FoDAppMicroserviceAndRelNameDescriptor(appMicroserviceAndRelNameArray[0], "", appMicroserviceAndRelNameArray[1]);

        }
    }
}