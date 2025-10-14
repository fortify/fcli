package com.fortify.cli.common.output.cli.cmd;

import com.fortify.cli.common.json.record.IRecordProducer;

public interface IRecordProducerSupplier {
    IRecordProducer getRecordProducer();
}
