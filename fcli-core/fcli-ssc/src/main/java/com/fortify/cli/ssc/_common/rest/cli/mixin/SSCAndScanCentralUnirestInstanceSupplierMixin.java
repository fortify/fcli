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

import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.ssc._common.rest.helper.SSCAndScanCentralUnirestHelper;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCSessionNameArgGroup;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;

public class SSCAndScanCentralUnirestInstanceSupplierMixin extends AbstractSessionDescriptorSupplierMixin<SSCAndScanCentralSessionDescriptor> {
    @Getter @ArgGroup(headingKey = "ssc.session.name.arggroup") 
    private SSCSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected final SSCAndScanCentralSessionDescriptor getSessionDescriptor(String sessionName) {
        return SSCAndScanCentralSessionHelper.instance().get(sessionName, true);
    }
    
    public final UnirestInstance getSscUnirestInstance() {
        return GenericUnirestFactory.getUnirestInstance("ssc/"+getSessionName(),
                u->SSCAndScanCentralUnirestHelper.configureSscUnirestInstance(u, getSessionDescriptor()));
    }

    public final UnirestInstance getScSastUnirestInstance() {
        return GenericUnirestFactory.getUnirestInstance("sc-sast/"+getSessionName(),
                u->SSCAndScanCentralUnirestHelper.configureScSastControllerUnirestInstance(u, getSessionDescriptor()));
    }
    
    public final UnirestInstance getScDastUnirestInstance() {
        return GenericUnirestFactory.getUnirestInstance("sc-dast/"+getSessionName(),
                u->SSCAndScanCentralUnirestHelper.configureScDastControllerUnirestInstance(u, getSessionDescriptor()));
    }
    
    public static final void shutdownUnirestInstance(String sessionName) {
        GenericUnirestFactory.shutdown("ssc/"+sessionName);
        GenericUnirestFactory.shutdown("sc-sast/"+sessionName);
        GenericUnirestFactory.shutdown("sc-dast/"+sessionName);
    }
}
