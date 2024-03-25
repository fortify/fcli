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
package com.fortify.cli.sc_sast._common.session.cli.mixin;

import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.sc_sast._common.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast._common.session.helper.SCSastSessionDescriptor;
import com.fortify.cli.sc_sast._common.session.helper.SCSastSessionHelper;

import kong.unirest.UnirestInstance;

public class SCSastUnirestInstanceSupplierMixin extends AbstractSessionDescriptorSupplierMixin<SCSastSessionDescriptor> {
    @Override
    protected final SCSastSessionDescriptor getSessionDescriptor(String sessionName) {
        return SCSastSessionHelper.instance().get(sessionName, true);
    }
    
    public final UnirestInstance getSscUnirestInstance() {
        return GenericUnirestFactory.getUnirestInstance("sc-sast/ssc/"+getSessionName(),
                u->SCSastUnirestHelper.configureSscUnirestInstance(u, getSessionDescriptor()));
    }

    public final UnirestInstance getControllerUnirestInstance() {
        return GenericUnirestFactory.getUnirestInstance("sc-sast/ctrl/"+getSessionName(),
                u->SCSastUnirestHelper.configureScSastControllerUnirestInstance(u, getSessionDescriptor()));
    }
}
