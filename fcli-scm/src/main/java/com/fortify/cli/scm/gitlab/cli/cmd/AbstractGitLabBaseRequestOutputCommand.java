package com.fortify.cli.scm.gitlab.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public abstract class AbstractGitLabBaseRequestOutputCommand extends AbstractGitLabOutputCommand  implements IBaseRequestSupplier {
    @Override
    public final HttpRequest<?> getBaseRequest() {
        try ( var unirest = getProductHelper().createUnirestInstance() ) {
            return getBaseRequest(unirest);
        }
    }

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest);
}
