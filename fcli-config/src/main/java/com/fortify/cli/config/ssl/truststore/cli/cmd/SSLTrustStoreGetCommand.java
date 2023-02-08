package com.fortify.cli.config.ssl.truststore.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.config.ssl.truststore.helper.TrustStoreOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=BasicOutputHelperMixins.Get.CMD_NAME)
public class SSLTrustStoreGetCommand extends AbstractBasicOutputCommand implements IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Get outputHelper;
    
    @Override
    protected JsonNode getJsonNode() {
    	return TrustStoreConfigHelper.getTrustStoreConfig().asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return TrustStoreOutputHelper::transformRecord;
    }
}
