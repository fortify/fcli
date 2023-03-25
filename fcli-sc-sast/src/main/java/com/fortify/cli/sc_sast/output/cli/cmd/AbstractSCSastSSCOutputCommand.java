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
