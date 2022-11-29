package com.fortify.cli.common.rest.runner;

import java.util.function.BiFunction;

import com.fortify.cli.common.session.manager.api.ISessionData;

import kong.unirest.UnirestInstance;

public interface IUnirestWithSessionDataRunner<D extends ISessionData> {
    <R> R run(BiFunction<UnirestInstance, D, R> f);
}