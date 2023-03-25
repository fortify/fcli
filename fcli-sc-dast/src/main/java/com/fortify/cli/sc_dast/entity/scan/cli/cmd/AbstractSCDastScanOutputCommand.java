package com.fortify.cli.sc_dast.entity.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.sc_dast.entity.scan.helper.SCDastScanStatus;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastOutputCommand;

public abstract class AbstractSCDastScanOutputCommand extends AbstractSCDastOutputCommand implements IRecordTransformer {
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SCDastScanStatus.addScanStatus(record);
    }
}