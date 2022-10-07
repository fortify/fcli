package com.fortify.cli.common.output.cli.mixin.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Model.CommandSpec;

public interface IOutputHelper extends IBasicOutputConfigSupplier, IOutputWriterFactorySupplier {
    public IProductHelper getProductHelper();
    public CommandSpec getCommandSpec();
    public <T> T getCommandAs(Class<T> asType);
    void write(UnirestInstance unirest, HttpRequest<?> baseRequest);
    void write(UnirestInstance unirest, JsonNodeHolder jsonNodeHolder);
    void write(UnirestInstance unirest, JsonNode jsonNode);
}
