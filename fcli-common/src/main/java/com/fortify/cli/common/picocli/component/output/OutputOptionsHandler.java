package com.fortify.cli.common.picocli.component.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.transform.fields.PredefinedFieldsTransformerFactory;
import com.fortify.cli.common.json.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.json.transform.jsonpath.JsonPathTransformer;
import com.fortify.cli.common.output.IOutputWriter;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.OutputFormat.OutputType;
import com.fortify.cli.common.output.OutputWriterConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class OutputOptionsHandler {
	@Spec(Spec.Target.MIXEE) CommandSpec mixee;

    @CommandLine.Option(names = {"--fmt", "--format"},
            description = "Output format. Possible values: ${COMPLETION-CANDIDATES}.",
            order=1)
    private OutputFormat outputFormat;

    @CommandLine.Option(names = {"--fields"},
            description = "Define the fields to be included in the output, together with optional header names",
            order=2)
    private String fields;
    
    @CommandLine.Option(names = {"--flatten"},
            description = "For non-column-based outputs, whether to flatten the structure",
            order=3, defaultValue = "false")
    @Getter
    private boolean flatten;
    
    @CommandLine.Option(names = {"--with-headers"},
            description = "For column-based outputs, whether to output headers",
            order=3, defaultValue = "false")
    @Getter
    private boolean withHeaders;

	@CommandLine.Option(names = {"--json-path"}, description = "Transforms output using JSONPath", order = 1)
	@Getter private String jsonPath;

    public void write(JsonNode response) {
    	OutputFormat format = getOutputFormat();
        getOutputWriter(format).write(transform(response, format));
    }
    
    protected OutputFormat getOutputFormat() {
    	Object mixeeUserObject = mixee.userObject();
    	OutputFormat result = this.outputFormat;
    	if ( result == null && mixeeUserObject instanceof IDefaultOutputFormatSupplier ) {
    		result = ((IDefaultOutputFormatSupplier)mixeeUserObject).getDefaultOutputFormat();
    	}
    	if ( result == null ) {
    		result = OutputFormat.table;
    	}
    	return result;
    }

	private IOutputWriter getOutputWriter(OutputFormat outputFormat) {
		return outputFormat.getOutputWriterFactory().createOutputWriter(createConfig());
	}

	private OutputWriterConfig createConfig() {
		return OutputWriterConfig.builder()
				// TODO .writerSupplier(null)
				.headersEnabled(isWithHeaders())
				.build();
	}
	
	protected JsonNode transform(JsonNode data, OutputFormat outputFormat) {
		data = applyMixeeTransformation(data, outputFormat);
		data = applyJsonPathTransformation(data, outputFormat);
		data = applyFieldsTransformation(data, outputFormat);
		data = applyFlattenTransformation(data, outputFormat);
		return data;
	}
	
	protected JsonNode applyMixeeTransformation(JsonNode data, OutputFormat outputFormat) {
		Object mixeeUserObject = mixee.userObject();
		if ( mixeeUserObject instanceof IOutputPreTransformer ) {
			data = ((IOutputPreTransformer)mixeeUserObject).transform(outputFormat, data);
		}
		return data;
	}
	
	protected JsonNode applyJsonPathTransformation(JsonNode data, OutputFormat outputFormat) {
		if ( StringUtils.isNotEmpty(jsonPath) ) {
			data = new JsonPathTransformer(jsonPath).transform(data);
		}
		return data;
	}
	
	protected JsonNode applyFieldsTransformation(JsonNode data, OutputFormat outputFormat) {
		String _fields = getFields(outputFormat);
		if ( StringUtils.isNotEmpty(_fields) && !"all".equals(_fields)) {
			data = PredefinedFieldsTransformerFactory.createFromString(outputFormat.getFieldNameFormatter(), _fields).transform(data);
		} else if ( outputFormat.getOutputType()==OutputType.TEXT_COLUMNS ) {
			data = new FlattenTransformer(outputFormat.getFieldNameFormatter(), ".", false).transform(data);
		}
		return data;
	}
	
	protected JsonNode applyFlattenTransformation(JsonNode data, OutputFormat outputFormat2) {
		if ( flatten ) {
			data = new FlattenTransformer(outputFormat.getFieldNameFormatter(), ".", false).transform(data);
		}
		return data;
	}

	private String getFields(OutputFormat outputFormat) {
		String _fields = fields;
		if ( StringUtils.isEmpty(_fields) ) {
			Object mixeeUserObject = mixee.userObject();
			if ( mixeeUserObject instanceof IDefaultOutputFieldsSupplier ) {
				_fields = ((IDefaultOutputFieldsSupplier)mixeeUserObject).getDefaultOutputFields(outputFormat);
			} else if ( outputFormat.getOutputType()==OutputType.TEXT_COLUMNS && mixeeUserObject instanceof IDefaultOutputColumnsSupplier ) {
				_fields = ((IDefaultOutputColumnsSupplier)mixeeUserObject).getDefaultOutputColumns(outputFormat);
			}
		}
		return _fields;
	}
}


