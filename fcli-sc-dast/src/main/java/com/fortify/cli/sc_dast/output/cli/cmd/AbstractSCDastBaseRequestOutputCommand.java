package com.fortify.cli.sc_dast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public abstract class AbstractSCDastBaseRequestOutputCommand extends AbstractSCDastOutputCommand  implements IBaseRequestSupplier {
    @Override
    public final HttpRequest<?> getBaseRequest() {
        return getBaseRequest(getUnirestInstance());
    }

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest);
}
