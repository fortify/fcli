package com.fortify.cli.common.rest.runner;

import java.util.function.Function;

import kong.unirest.UnirestInstance;

public interface IUnirestRunner {
    <R> R run(Function<UnirestInstance, R> f);
}