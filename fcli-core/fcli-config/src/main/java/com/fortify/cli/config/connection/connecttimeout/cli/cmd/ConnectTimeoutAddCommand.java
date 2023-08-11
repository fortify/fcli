package com.fortify.cli.config.connection.connecttimeout.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.connection.helper.ConnectionHelper;
import com.fortify.cli.common.http.connection.helper.TimeoutDescriptor.TimeoutType;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.config.connection.cli.mixin.TimeoutOptions;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name=OutputHelperMixins.Add.CMD_NAME)
public class ConnectTimeoutAddCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Mixin @Getter private OutputHelperMixins.Add outputHelper;
    @Mixin private TimeoutOptions timeoutOptions;
    
    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public JsonNode getJsonNode() {
        return ConnectionHelper.addTimeout(timeoutOptions.asTimeoutDescriptor(TimeoutType.CONNECT)).asJsonNode();
    }

}
