package com.fortify.cli.state._main.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.FcliHomeHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

// TODO Remove code duplication between this class and ConfigClearCommand
@Command(name = BasicOutputHelperMixins.Clear.CMD_NAME)
public class StateClearCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    @Getter @Mixin private BasicOutputHelperMixins.Clear outputHelper;
    @Option(names={"-y", "--confirm"}, required = true) private boolean confirm;
    
    @Override
    protected JsonNode getJsonNode() {
        ArrayNode result = objectMapper.createArrayNode();
        try {
            if ( FcliHomeHelper.getFcliStatePath().toFile().exists() ) {
                Files.walk(FcliHomeHelper.getFcliStatePath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(f->addResult(result,f))
                    .forEach(File::delete);
            }
        } catch ( IOException e ) {
            throw new RuntimeException("Error clearing fcli state directory", e);
        }
        return result;
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private void addResult(ArrayNode result, File f) {
        try {
            result.add(objectMapper.createObjectNode()
                    .put("name", f.getCanonicalPath())
                    .put("type", f.isFile() ? "FILE" : "DIR"));
        } catch ( IOException e ) {
            throw new RuntimeException("Error processing file "+f, e);
        }
    }
}
