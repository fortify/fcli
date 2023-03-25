package com.fortify.cli.common.session.helper;

public interface ISessionDescriptorSupplier<D extends ISessionDescriptor> {
    D getSessionDescriptor();
}
