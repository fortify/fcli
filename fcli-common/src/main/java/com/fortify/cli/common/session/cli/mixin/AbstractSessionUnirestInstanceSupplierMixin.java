/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
