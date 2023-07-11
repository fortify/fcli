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
public final class FoDAppAndRelNameDescriptor {
    private final String appName, relName;
    
    public static final FoDAppAndRelNameDescriptor fromCombinedAppAndRelName(String appAndRelName, String delimiter) {
        String[] appAndRelNameArray = appAndRelName.split(delimiter);
        if (appAndRelNameArray.length != 2) {
            throw new ValidationException("Application and release name must be specified in the format <application name>"+delimiter+"<release name>");
        }
        return new FoDAppAndRelNameDescriptor(appAndRelNameArray[0], appAndRelNameArray[1]);
    }
}