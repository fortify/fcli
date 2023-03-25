package com.fortify.cli.common.session.cli.mixin;

import com.fortify.cli.common.session.helper.ISessionDescriptor;
import com.fortify.cli.common.session.helper.ISessionDescriptorSupplier;

import picocli.CommandLine.Mixin;

public abstract class AbstractSessionDescriptorSupplierMixin<D extends ISessionDescriptor> implements ISessionDescriptorSupplier<D> {
    @Mixin private SessionNameMixin.OptionalOption sessionNameMixin;

    public final D getSessionDescriptor() {
        return getSessionDescriptor(getSessionName());
    }
    
    public final String getSessionName() {
        return sessionNameMixin.getSessionName();
    }
    
    protected abstract D getSessionDescriptor(String sessionName);
}
