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
package com.fortify.cli.common.session.cli.mixin;

import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.session.helper.ISessionDescriptor;
import com.fortify.cli.common.session.helper.ISessionDescriptorSupplier;

public abstract class AbstractSessionDescriptorSupplierMixin<D extends ISessionDescriptor> implements ISessionNameSupplier, ISessionDescriptorSupplier<D> {
    public final D getSessionDescriptor() {
        var transientSessionDescriptor = getTransientSessionDescriptor();
        return transientSessionDescriptor != null ? transientSessionDescriptor : getSessionDescriptor(getSessionName());
    }

    public final String getSessionName() {
        var sessionNameSupplier = getSessionNameSupplier();
        return sessionNameSupplier==null?"default":sessionNameSupplier.getSessionName();
    }

    @SuppressWarnings("unchecked")
    private D getTransientSessionDescriptor() {
        return (D)FcliExecutionContextHolder.current().getIsolationScope().getTransientSessionDescriptor(getSessionDescriptorType());
    }
    
    public abstract ISessionNameSupplier getSessionNameSupplier();
    protected abstract String getSessionDescriptorType();

    protected abstract D getSessionDescriptor(String sessionName);
}
