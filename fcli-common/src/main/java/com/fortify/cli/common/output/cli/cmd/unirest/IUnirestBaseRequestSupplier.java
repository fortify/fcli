package com.fortify.cli.common.output.cli.cmd.unirest;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IUnirestBaseRequestSupplier {
    HttpRequest<?> getBaseRequest(UnirestInstance unirest);
}
