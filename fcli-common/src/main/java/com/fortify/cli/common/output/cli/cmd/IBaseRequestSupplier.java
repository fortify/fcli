package com.fortify.cli.common.output.cli.cmd;

import kong.unirest.HttpRequest;

public interface IBaseRequestSupplier {
    HttpRequest<?> getBaseRequest();
}
