package com.fortify.cli.common.output.cli.mixin.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IOutputHelper extends IBasicOutputConfigSupplier, IOutputWriterFactorySupplier {
    public IProductHelper getProductHelper();
    void write(UnirestInstance unirest, HttpRequest<?> baseRequest);
    void write(UnirestInstance unirest, JsonNodeHolder jsonNodeHolder);
    void write(UnirestInstance unirest, JsonNode jsonNode);
}
