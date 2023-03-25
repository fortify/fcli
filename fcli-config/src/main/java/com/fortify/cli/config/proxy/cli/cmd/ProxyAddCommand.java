package com.fortify.cli.config.proxy.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.config.proxy.cli.mixin.ProxyAddOptions;
import com.fortify.cli.config.proxy.helper.ProxyOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.Add.CMD_NAME)
public class ProxyAddCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IRecordTransformer {
    @Mixin @Getter private OutputHelperMixins.Add outputHelper;
    @Mixin private ProxyAddOptions proxyCreateOptions;
    
    @Override
    public JsonNode getJsonNode() {
        return ProxyHelper.addProxy(proxyCreateOptions.asProxyDescriptor()).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
        return ProxyOutputHelper.transformRecord(input);
    }
}
