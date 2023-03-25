package com.fortify.cli.common.session.cli.mixin;

import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

import kong.unirest.UnirestInstance;

public abstract class AbstractSessionUnirestInstanceSupplierMixin<D extends ISessionDescriptor> 
    extends AbstractSessionDescriptorSupplierMixin<D>
    implements IUnirestInstanceSupplier {
    
    @Override
    public UnirestInstance getUnirestInstance() {
        D sessionDescriptor = getSessionDescriptor();
        String key = this.getClass().getName()+"/"+getSessionName();
        UnirestInstance unirest = GenericUnirestFactory.getUnirestInstance(key);
        configure(unirest, sessionDescriptor);
        return unirest;
    }
    
    protected abstract void configure(UnirestInstance unirest, D sessionDescriptor);
}
