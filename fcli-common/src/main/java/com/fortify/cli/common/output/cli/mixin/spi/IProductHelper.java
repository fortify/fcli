package com.fortify.cli.common.output.cli.mixin.spi;

public interface IProductHelper extends IInputTransformerSupplier, INextPageUrlProducerSupplier, IRecordTransformerSupplier, IRequestUpdater {
    void setOutputHelper(IOutputHelper outputHelper);
}
