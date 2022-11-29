package com.fortify.cli.common.output.cli.cmd.unirest;

import com.fortify.cli.common.session.manager.api.ISessionData;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IUnirestWithSessionDataBaseRequestSupplier<D extends ISessionData> {
    HttpRequest<?> getBaseRequest(UnirestInstance unirest, D sessionData);
}
