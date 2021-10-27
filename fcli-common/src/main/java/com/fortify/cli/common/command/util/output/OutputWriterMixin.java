package com.fortify.cli.common.command.util.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.transformer.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transformer.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.OutputWriterConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class OutputWriterMixin {
	@Inject FieldBasedTransformerFactory fieldBasedTransformerFactory;
	@Spec(Spec.Target.MIXEE) CommandSpec mixee;

    @CommandLine.Option(names = {"--fmt", "--format"},
            description = "Output format. Possible values: ${COMPLETION-CANDIDATES}.",
            defaultValue = "json", required = false, order=1)
    @Getter
    private OutputFormat format;

    @CommandLine.Option(names = {"--fields"},
            description = "Define the fields to be included in the output, together with optional header names",
            required = false, order=2)
    @Getter
    private String fields;
    
    @CommandLine.Option(names = {"--with-headers"},
            description = "For column-based outputs, whether to output headers",
            required = false, order=3, defaultValue = "false")
    @Getter
    private boolean withHeaders;

    public void printToFormat(JsonNode response) {
        format.getOutputWriterFactory().createOutputWriter(createConfig()).write(response);
    }

	private OutputWriterConfig createConfig() {
		return OutputWriterConfig.builder()
				.mapper(getFieldMapper())
				.headersEnabled(isWithHeaders())
				.build();
	}

	private IJsonNodeTransformer getFieldMapper() {
		if ( StringUtils.isNotEmpty(fields) ) {
			return fieldBasedTransformerFactory.createFromString(format.getFieldNameFormatter(), fields);
		} else {
			Object cmd = mixee.userObject();
			if ( cmd instanceof IJsonNodeTransformerSupplier ) {
				return ((IJsonNodeTransformerSupplier)cmd).getJsonNodeTransformer(fieldBasedTransformerFactory, format);
			} else {
				throw new RuntimeException("Command class "+cmd.getClass()+" must implement IDefaultJacksonJsonNodeMapperSupplier");
			}
		}
	}

}
