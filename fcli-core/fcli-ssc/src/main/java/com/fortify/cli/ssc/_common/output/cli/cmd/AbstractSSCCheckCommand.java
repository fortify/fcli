/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc._common.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractCheckCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.ssc._common.rest.helper.SSCProductHelper;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCCheckCommand extends AbstractCheckCommand 
    implements IProductHelperSupplier, IUnirestInstanceSupplier
{
    @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Getter private final SSCProductHelper productHelper = SSCProductHelper.INSTANCE;
    
    public UnirestInstance getUnirestInstance() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
    
    @Override
    protected final boolean isPass() {
        return isPass(getUnirestInstance());
    }

    protected abstract boolean isPass(UnirestInstance unirest);
}
