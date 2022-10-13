package com.fortify.cli.common.output.spi.product;

import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;

public interface IProductHelper {
    void setOutputHelper(IUnirestOutputHelper unirestOutputHelper);
}
