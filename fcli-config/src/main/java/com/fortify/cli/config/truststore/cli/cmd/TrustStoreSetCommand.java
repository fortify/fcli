package com.fortify.cli.config.truststore.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
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
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name=BasicOutputHelperMixins.Set.CMD_NAME)
public class TrustStoreSetCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier, IRecordTransformerSupplier {
    @Mixin @Getter private BasicOutputHelperMixins.Set outputHelper;
    
    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.config.truststore.set.trustStorePath")
    private String trustStorePath;
    
    @Option(names = {"-p", "--truststore-password"})
    private String trustStorePassword;
    
    @Option(names = {"-t", "--truststore-type"}, defaultValue = "jks")
    private String trustStoreType;
    
    @Override
    protected JsonNode getJsonNode() {
    	Path absolutePath = Path.of(trustStorePath).toAbsolutePath();
    	if ( !Files.exists(absolutePath) ) {
    		throw new IllegalArgumentException("Trust store cannot be found: "+absolutePath);
    	}
		String absolutePathString = absolutePath.toString();
    	TrustStoreConfigDescriptor descriptor = TrustStoreConfigDescriptor.builder()
    		.path(absolutePathString)
    		.type(trustStoreType)
    		.password(trustStorePassword)
    		.build();
        TrustStoreConfigHelper.setTrustStoreConfig(descriptor);
        return descriptor.asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "CONFIGURED";
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return TrustStoreOutputHelper::transformRecord;
    }
}
