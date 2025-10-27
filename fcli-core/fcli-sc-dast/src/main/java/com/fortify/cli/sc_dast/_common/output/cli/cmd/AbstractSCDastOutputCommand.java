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
package com.fortify.cli.sc_dast._common.output.cli.cmd;

import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.ssc._common.rest.sc_dast.cli.mixin.SCDastUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.rest.sc_dast.helper.SCDastProductHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSCDastOutputCommand extends AbstractOutputCommand 
    implements IProductHelperSupplier, IUnirestInstanceSupplier
{
    @Mixin private SCDastUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Getter private final SCDastProductHelper productHelper = SCDastProductHelper.INSTANCE;
    
    public final UnirestInstance getUnirestInstance() {
        return unirestInstanceSupplier.getUnirestInstance();
    }

    @Override
    protected final IObjectNodeProducer getObjectNodeProducer() {
        return getObjectNodeProducer(getUnirestInstance());
    }

    /**
     * Overload allowing subclasses to access the DAST {@link UnirestInstance}.
     * Default implementation delegates to {@link AbstractOutputCommand#getLegacyObjectNodeProducer()}.
     * @param unirestInstance DAST Unirest instance
     * @return Object node producer (default: legacy behavior)
     */
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirestInstance) {
        return getLegacyObjectNodeProducer();
    }
}
