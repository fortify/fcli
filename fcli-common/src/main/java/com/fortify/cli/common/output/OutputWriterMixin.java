package com.fortify.cli.common.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.mapper.FieldMapperFactory;
import com.fortify.cli.common.json.mapper.IJacksonJsonNodeMapper;
import com.fortify.cli.common.output.writer.OutputWriterConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class OutputWriterMixin {
	@Inject FieldMapperFactory fieldMapperFactory;
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

    public void printToFormat(JsonNode response) {
        format.getOutputWriterFactory().createOutputWriter(createConfig()).write(response);
    }

	private OutputWriterConfig createConfig() {
		return OutputWriterConfig.builder()
				.mapper(getFieldMapper())
				.build();
	}

	private IJacksonJsonNodeMapper getFieldMapper() {
		if ( StringUtils.isNotEmpty(fields) ) {
			return fieldMapperFactory.createFromString(format.getOutputWriterFactory().getDefaultPropertyPathToHeaderMapper(), fields);
		} else {
			Object cmd = mixee.userObject();
			if ( cmd instanceof IDefaultJacksonJsonNodeMapperSupplier ) {
				return ((IDefaultJacksonJsonNodeMapperSupplier)cmd).getJacksonJsonNodeMapper(fieldMapperFactory, format);
			} else {
				throw new RuntimeException("Command class "+cmd.getClass()+" must implement IDefaultJacksonJsonNodeMapperSupplier");
			}
		}
	}

}
