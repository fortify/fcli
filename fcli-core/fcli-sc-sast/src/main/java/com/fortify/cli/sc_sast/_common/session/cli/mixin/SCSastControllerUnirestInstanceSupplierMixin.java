/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.sc_sast._common.session.cli.mixin;

import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;

import kong.unirest.UnirestInstance;

public class SCSastControllerUnirestInstanceSupplierMixin extends SCSastUnirestInstanceSupplierMixin implements IUnirestInstanceSupplier {
    @Override
    public UnirestInstance getUnirestInstance() {
        return getControllerUnirestInstance();
    }
}
