package com.fortify.cli.common.rest.runner.config;

import kong.unirest.UnirestInstance;

public class UnirestJsonHeaderConfigurer {
    public static final void configure(UnirestInstance unirest) {
        unirest.config()
            .setDefaultHeader("Accept", "application/json")
            .setDefaultHeader("Content-Type", "application/json");
    }
}
