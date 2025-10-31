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
package com.fortify.cli.sc_sast._common.output.cli.cmd;

import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.ssc._common.rest.cli.mixin.SSCAndScanCentralUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.rest.sc_sast.helper.SCSastProductHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSCSastOutputCommand extends AbstractOutputCommand 
    implements IProductHelperSupplier, IUnirestInstanceSupplier
{
    @Getter @Mixin private SSCAndScanCentralUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Getter private final SCSastProductHelper productHelper = SCSastProductHelper.INSTANCE;
    
    public final UnirestInstance getUnirestInstance() {
        return unirestInstanceSupplier.getScSastUnirestInstance();
    }
    
    protected final UnirestInstance getSscUnirestInstance() {
        return unirestInstanceSupplier.getSscUnirestInstance();
    }

    @Override
    protected final IObjectNodeProducer getObjectNodeProducer() {
        return getObjectNodeProducer(getUnirestInstance());
    }

    /**
     * Overload allowing subclasses to access the SAST {@link UnirestInstance}. Subclasses can use
     * both SAST and SSC instances if needed via {@link #getSscUnirestInstance()}.
     * Default implementation delegates to {@link AbstractOutputCommand#getLegacyObjectNodeProducer()}.
     * @param unirestInstance SAST Unirest instance
     * @return Object node producer (default: legacy behavior)
     */
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirestInstance) {
        return getLegacyObjectNodeProducer();
    }
}
