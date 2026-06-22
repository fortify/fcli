/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod._common.session.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.fod._common.rest.helper.FoDUnirestHelper;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

public final class FoDUnirestInstanceSupplierMixin extends AbstractSessionDescriptorSupplierMixin<FoDSessionDescriptor> implements IUnirestInstanceSupplier
{   
    @Getter @ArgGroup(headingKey = "fod.session.name.arggroup") 
    private FoDSessionNameArgGroup sessionNameSupplier;
    
    @Mixin private UnirestContextMixin unirestContextMixin;
    
    @Override
    protected final FoDSessionDescriptor getSessionDescriptor(String sessionName) {
        return FoDSessionHelper.instance().get(sessionName, true);
    }

    @Override
    protected String getSessionDescriptorType() {
        return FoDSessionHelper.instance().getType();
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        FoDSessionDescriptor sessionDescriptor = getSessionDescriptor();
        String key = "fod/"+getSessionName();
        return unirestContextMixin.getUnirestInstance(key, u->configure(u, sessionDescriptor));
    }
    
    public final void close(String sessionName) {
        unirestContextMixin.close("fod/"+sessionName);
    }

    protected final void configure(UnirestInstance unirest, FoDSessionDescriptor sessionDescriptor) {
        FoDUnirestHelper.configureUnirestInstance(unirest, sessionDescriptor);
    }
}