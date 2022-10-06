package com.fortify.cli.common.output.cli.cmd;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IBaseHttpRequestSupplier {
    HttpRequest<?> getBaseRequest(UnirestInstance unirest);
}
