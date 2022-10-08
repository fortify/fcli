package com.fortify.cli.common.output.cli.cmd;

import com.fortify.cli.common.json.JsonNodeHolder;

import kong.unirest.UnirestInstance;

public interface IJsonNodeHolderSupplier {
    JsonNodeHolder getJsonNodeHolder(UnirestInstance unirest);
}
