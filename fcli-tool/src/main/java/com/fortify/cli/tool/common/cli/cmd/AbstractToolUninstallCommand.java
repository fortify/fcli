package com.fortify.cli.tool.common.cli.cmd;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.tool.common.helper.ToolHelper;
import com.fortify.cli.tool.common.helper.ToolVersionCombinedDescriptor;
import com.fortify.cli.tool.common.util.FileUtils;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

public abstract class AbstractToolUninstallCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier {
    @Getter @Parameters(index="0", arity="1", descriptionKey="fcli.tool.uninstall.version") 
    private String version;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    
    @Override
    protected final JsonNode getJsonNode() {
        String toolName = getToolName();
        ToolVersionCombinedDescriptor descriptor = ToolHelper.loadToolVersionCombinedDescriptor(toolName, version);
        if ( descriptor==null ) {
            throw new IllegalArgumentException("Tool installation not found");
        }
        Path installPath = descriptor.getInstallPath();
        if ( installPath==null ) {
            throw new IllegalStateException("Tool installation path not found");
        }
        requireConfirmation.checkConfirmed();
        try {
            FileUtils.deleteRecursive(installPath);
        } catch ( IOException e ) {
            throw new RuntimeException("Error deleting tool installation; please manually delete the "+installPath+" directory", e);
        } finally {
            ToolHelper.deleteToolVersionInstallDescriptor(toolName, version);
        }
        return new ObjectMapper().valueToTree(descriptor);
    }
    
    @Override
    public String getActionCommandResult() {
        return "UNINSTALLED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    protected abstract String getToolName();
}
