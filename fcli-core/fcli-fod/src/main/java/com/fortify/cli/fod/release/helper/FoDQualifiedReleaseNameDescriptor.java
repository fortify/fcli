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

package com.fortify.cli.fod.release.helper;

import javax.validation.ValidationException;

import lombok.Data;

@Data
public final class FoDQualifiedReleaseNameDescriptor {
    private final String appName, microserviceName, releaseName;
    
    public static final FoDQualifiedReleaseNameDescriptor fromQualifiedReleaseName(String qualifiedReleaseName, String delimiter) {
        String[] elts = qualifiedReleaseName.split(delimiter);
        switch ( elts.length ) {
        case 3: return new FoDQualifiedReleaseNameDescriptor(elts[0], elts[1], elts[2]);
        case 2: return new FoDQualifiedReleaseNameDescriptor(elts[0], null, elts[1]);
        default: throw new ValidationException("Release name must be specified in the format <application name>["+delimiter+"<microservice name>]"+delimiter+"<release name>");
        }
    }
}