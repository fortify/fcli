package com.fortify.cli.config.proxy.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.config.proxy.helper.ProxyOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=BasicOutputHelperMixins.Clear.CMD_NAME)
public class ProxyClearCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier, IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Clear outputHelper;
    
    @Override
    protected JsonNode getJsonNode() {
        return ProxyHelper.deleteAllProxies().map(ProxyDescriptor::asJsonNode).collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return ProxyOutputHelper::transformRecord;
    }
}
