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
