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

package com.fortify.cli.fod.app.helper;

import lombok.Data;

@Data
public final class FoDMicroserviceAndReleaseNameDescriptor {
    private final String microserviceName, releaseName;
    
    public static final FoDMicroserviceAndReleaseNameDescriptor fromMicroserviceAndReleaseName(String microserviceAndReleaseName, String delimiter) {
        String[] elts = microserviceAndReleaseName.split(delimiter);
        switch ( elts.length ) {
        case 2: return new FoDMicroserviceAndReleaseNameDescriptor(elts[0], elts[1]);
        case 1: return new FoDMicroserviceAndReleaseNameDescriptor(null, elts[0]);
        default: throw new IllegalArgumentException("Release name must be specified in the format ["+delimiter+"<microservice name>]"+delimiter+"<release name>");
        }
    }
}