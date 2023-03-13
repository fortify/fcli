package com.fortify.cli.config.truststore.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.config.truststore.helper.TrustStoreOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=BasicOutputHelperMixins.Clear.CMD_NAME)
public class TrustStoreClearCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier, IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Clear outputHelper;
    
    @Override
    protected JsonNode getJsonNode() {
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
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return TrustStoreOutputHelper::transformRecord;
    }
}
