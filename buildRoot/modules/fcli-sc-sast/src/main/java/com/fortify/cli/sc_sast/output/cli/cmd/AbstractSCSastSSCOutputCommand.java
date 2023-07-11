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
package com.fortify.cli.sc_sast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastSSCProductHelperMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSCSastSSCOutputCommand extends AbstractOutputCommand 
    implements IProductHelperSupplier, IUnirestInstanceSupplier
{
    @Getter @Mixin SCSastSSCProductHelperMixin productHelper;
    
    public final UnirestInstance getUnirestInstance() {
        return productHelper.getUnirestInstance();
    }
    
    protected final UnirestInstance getControllerUnirestInstance() {
        return productHelper.getControllerUnirestInstance();
    }
}
