package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastOutputCommand;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;

public abstract class AbstractSCDastScanOutputCommand extends AbstractSCDastOutputCommand implements IRecordTransformer {
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SCDastScanStatus.addScanStatus(record);
    }
}