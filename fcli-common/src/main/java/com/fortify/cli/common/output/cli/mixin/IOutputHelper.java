package com.fortify.cli.common.output.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.writer.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.writer.IOutputWriterFactorySupplier;

import kong.unirest.HttpRequest;

public interface IOutputHelper extends IBasicOutputConfigSupplier, IOutputWriterFactorySupplier {
    IProductHelper getProductHelper();
    void write(HttpRequest<?> baseRequest);
    void write(JsonNodeHolder jsonNodeHolder);
    void write(JsonNode jsonNode);
}
