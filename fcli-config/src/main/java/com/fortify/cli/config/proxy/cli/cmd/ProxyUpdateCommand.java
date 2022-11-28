package com.fortify.cli.config.proxy.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.config.proxy.cli.mixin.ProxyUpdateOptions;
import com.fortify.cli.config.proxy.helper.ProxyOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=BasicOutputHelperMixins.Update.CMD_NAME)
public class ProxyUpdateCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier, IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Update outputHelper;
    @Mixin private ProxyUpdateOptions proxyUpdateOptions;
    
    @Override
    protected JsonNode getJsonNode() {
        return ProxyHelper.updateProxy(proxyUpdateOptions.asProxyDescriptor()).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return ProxyOutputHelper::transformRecord;
    }
}
