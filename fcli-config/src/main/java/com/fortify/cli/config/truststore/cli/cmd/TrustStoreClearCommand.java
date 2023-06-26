package com.fortify.cli.config.truststore.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.config.truststore.helper.TrustStoreOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.Clear.CMD_NAME)
public class TrustStoreClearCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier, IRecordTransformer {
    @Mixin @Getter private OutputHelperMixins.Clear outputHelper;
    
    @Override
    public JsonNode getJsonNode() {
    	TrustStoreConfigDescriptor descriptor = TrustStoreConfigHelper.getTrustStoreConfig();
        TrustStoreConfigHelper.clearTrustStoreConfig();
        return descriptor.asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "CLEARED";
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
        return TrustStoreOutputHelper.transformRecord(input);
    }
}
