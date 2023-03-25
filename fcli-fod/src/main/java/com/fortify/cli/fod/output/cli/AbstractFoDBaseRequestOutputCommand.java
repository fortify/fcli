package com.fortify.cli.fod.output.cli;

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public abstract class AbstractFoDBaseRequestOutputCommand extends AbstractFoDOutputCommand  implements IBaseRequestSupplier {
    @Override
    public final HttpRequest<?> getBaseRequest() {
        return getBaseRequest(getUnirestInstance());
    }

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest);
}
