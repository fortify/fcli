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
package com.fortify.cli.sc_sast.session.cli.mixin;

import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionDescriptor;
import com.fortify.cli.sc_sast.session.helper.SCSastSessionHelper;

import kong.unirest.UnirestInstance;

public abstract class AbstractSCSastUnirestInstanceSupplierMixin extends AbstractSessionDescriptorSupplierMixin<SCSastSessionDescriptor> {
    @Override
    protected final SCSastSessionDescriptor getSessionDescriptor(String sessionName) {
        return SCSastSessionHelper.instance().get(sessionName, true);
    }
    
    public final UnirestInstance getSscUnirestInstance() {
        UnirestInstance unirest = GenericUnirestFactory.getUnirestInstance("sc-sast/ssc/"+getSessionName());
        SCSastUnirestHelper.configureSscUnirestInstance(unirest, getSessionDescriptor());
        return unirest;
    }

    public final UnirestInstance getControllerUnirestInstance() {
        UnirestInstance unirest = GenericUnirestFactory.getUnirestInstance("sc-sast/cntrl/"+getSessionName());
        SCSastUnirestHelper.configureScSastControllerUnirestInstance(unirest, getSessionDescriptor());
        return unirest;
    }
}
