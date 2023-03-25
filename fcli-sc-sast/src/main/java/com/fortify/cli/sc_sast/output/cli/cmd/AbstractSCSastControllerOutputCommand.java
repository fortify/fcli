package com.fortify.cli.sc_sast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerProductHelperMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSCSastControllerOutputCommand extends AbstractOutputCommand 
    implements IProductHelperSupplier, IUnirestInstanceSupplier
{
    @Getter @Mixin SCSastControllerProductHelperMixin productHelper;
    
    public final UnirestInstance getUnirestInstance() {
        return productHelper.getUnirestInstance();
    }
    
    protected final UnirestInstance getSscUnirestInstance() {
        return productHelper.getSscUnirestInstance();
    }
}
