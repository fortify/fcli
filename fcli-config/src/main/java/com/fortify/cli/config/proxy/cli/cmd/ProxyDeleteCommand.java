package com.fortify.cli.config.proxy.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.config.proxy.helper.ProxyOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name=BasicOutputHelperMixins.Delete.CMD_NAME)
public class ProxyDeleteCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier, IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Delete outputHelper;
    @Parameters(arity="1", descriptionKey = "fcli.config.proxy.delete.name", paramLabel = "NAME")
    private String name;
    
    @Override
    protected JsonNode getJsonNode() {
        return ProxyHelper.deleteProxy(ProxyHelper.getProxy(name)).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
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
