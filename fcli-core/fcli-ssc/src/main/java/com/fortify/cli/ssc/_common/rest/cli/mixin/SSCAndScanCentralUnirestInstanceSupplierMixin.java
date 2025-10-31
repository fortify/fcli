/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.ssc._common.rest.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.ssc._common.rest.helper.SSCAndScanCentralUnirestHelper;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCSessionNameArgGroup;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

public class SSCAndScanCentralUnirestInstanceSupplierMixin extends AbstractSessionDescriptorSupplierMixin<SSCAndScanCentralSessionDescriptor> {
    @Getter @ArgGroup(headingKey = "ssc.session.name.arggroup") 
    private SSCSessionNameArgGroup sessionNameSupplier;
    @Mixin private UnirestContextMixin unirestContextMixin;
    
    @Override
    protected final SSCAndScanCentralSessionDescriptor getSessionDescriptor(String sessionName) {
        return SSCAndScanCentralSessionHelper.instance().get(sessionName, true);
    }
    
    public final UnirestInstance getSscUnirestInstance() {
        return unirestContextMixin.getUnirestInstance("ssc/"+getSessionName(),
                u->SSCAndScanCentralUnirestHelper.configureSscUnirestInstance(u, getSessionDescriptor()));
    }

    public final UnirestInstance getScSastUnirestInstance() {
        return unirestContextMixin.getUnirestInstance("sc-sast/"+getSessionName(),
                u->SSCAndScanCentralUnirestHelper.configureScSastControllerUnirestInstance(u, getSessionDescriptor()));
    }
    
    public final UnirestInstance getScDastUnirestInstance() {
        return unirestContextMixin.getUnirestInstance("sc-dast/"+getSessionName(),
                u->SSCAndScanCentralUnirestHelper.configureScDastControllerUnirestInstance(u, getSessionDescriptor()));
    }
    
    public final void close(String sessionName) {
        unirestContextMixin.close("ssc/"+sessionName);
        unirestContextMixin.close("sc-sast/"+sessionName);
        unirestContextMixin.close("sc-dast/"+sessionName);
    }
}
