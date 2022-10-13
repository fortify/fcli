package com.fortify.cli.common.output.cli.mixin.spi.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.spi.IOutputHelperBase;

public interface IBasicOutputHelper extends IOutputHelperBase {
    void write(JsonNode jsonNode);
}
