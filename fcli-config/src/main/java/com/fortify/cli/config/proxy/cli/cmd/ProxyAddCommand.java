package com.fortify.cli.config.proxy.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.config.proxy.cli.mixin.ProxyAddOptions;
import com.fortify.cli.config.proxy.helper.ProxyOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=BasicOutputHelperMixins.Add.CMD_NAME)
public class ProxyAddCommand extends AbstractBasicOutputCommand implements IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Add outputHelper;
    @Mixin private ProxyAddOptions proxyCreateOptions;
    
    @Override
    protected JsonNode getJsonNode() {
        return ProxyHelper.addProxy(proxyCreateOptions.asProxyDescriptor()).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return ProxyOutputHelper::transformRecord;
    }
}
