package com.fortify.cli.common.output.cli.cmd;

import java.util.function.Consumer;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Implemented by commands that allow external code (like FcliCommandExecutorFactory)
 * to collect per-record output without relying on global static state.
 */
public interface IRecordCollectionSupport {
    void setRecordConsumer(Consumer<ObjectNode> consumer, boolean suppressStdout);
    Consumer<ObjectNode> getRecordConsumer();
    boolean isStdoutSuppressedForRecordCollection();
}
