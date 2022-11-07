package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;

public abstract class AbstractSSCAppVersionArtifactOutputCommand extends AbstractSSCOutputCommand implements IRecordTransformer {
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionArtifactHelper.addScanTypes(record);
    }
}