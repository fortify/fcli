package com.fortify.cli.common.output.cli.mixin.spi.unirest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.cli.mixin.spi.IOutputHelperBase;
import com.fortify.cli.common.output.spi.product.IProductHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IUnirestOutputHelper extends IOutputHelperBase {
    IProductHelper getProductHelper();
    void write(UnirestInstance unirest, HttpRequest<?> baseRequest);
    void write(UnirestInstance unirest, JsonNodeHolder jsonNodeHolder);
    void write(UnirestInstance unirest, JsonNode jsonNode);
}
