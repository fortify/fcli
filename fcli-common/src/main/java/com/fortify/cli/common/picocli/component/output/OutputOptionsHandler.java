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
            defaultValue = "json", order=1)
    @Getter
    private OutputFormat format;

    @CommandLine.Option(names = {"--fields"},
            description = "Define the fields to be included in the output, together with optional header names",
            order=2)
    private String fields;
    
    @CommandLine.Option(names = {"--with-headers"},
            description = "For column-based outputs, whether to output headers",
            order=3, defaultValue = "false")
    @Getter
    private boolean withHeaders;

	@CommandLine.Option(names = {"--json-path"}, description = "Transforms output using JSONPath", order = 1)
	@Getter private String jsonPath;

    public void write(JsonNode response) {
        getOutputWriter().write(transform(response));
    }

	private IOutputWriter getOutputWriter() {
		return format.getOutputWriterFactory().createOutputWriter(createConfig());
	}

	private OutputWriterConfig createConfig() {
		return OutputWriterConfig.builder()
				// TODO .writerSupplier(null)
				.headersEnabled(isWithHeaders())
				.build();
	}
	
	protected JsonNode transform(JsonNode data) {
		data = applyMixeeTransformation(data);
		data = applyJsonPathTransformation(data);
		data = applyFieldsTransformation(data);
		return data;
	}
	
	protected JsonNode applyMixeeTransformation(JsonNode data) {
		Object mixeeUserObject = mixee.userObject();
		if ( mixeeUserObject instanceof IOutputPreTransformer ) {
			data = ((IOutputPreTransformer)mixeeUserObject).transform(format, data);
		}
		return data;
	}
	
	protected JsonNode applyJsonPathTransformation(JsonNode data) {
		if ( StringUtils.isNotEmpty(jsonPath) ) {
			data = new JsonPathTransformer(jsonPath).transform(data);
		}
		return data;
	}
	
	protected JsonNode applyFieldsTransformation(JsonNode data) {
		String _fields = getFields();
		if ( StringUtils.isEmpty(_fields) || ("all".equals(_fields) && format.getOutputType()==OutputType.TEXT_COLUMNS) ) {
			data = new FlattenTransformer(format.getFieldNameFormatter(), ".", false).transform(data);
		} else if ( StringUtils.isNotEmpty(_fields) ) {
			// TODO do we really need to pass field name formatter here? Or can we just have the writer rename the fields?
			data = PredefinedFieldsTransformerFactory.createFromString(format.getFieldNameFormatter(), _fields).transform(data);
		}
		return data;
	}

	private String getFields() {
		String _fields = fields;
		if ( StringUtils.isEmpty(_fields) ) {
			Object mixeeUserObject = mixee.userObject();
			if ( mixeeUserObject instanceof IDefaultOutputFieldsSupplier ) {
				_fields = ((IDefaultOutputFieldsSupplier)mixeeUserObject).getDefaultOutputFields(format);
			} else if ( format.getOutputType()==OutputType.TEXT_COLUMNS && mixeeUserObject instanceof IDefaultOutputColumnsSupplier ) {
				_fields = ((IDefaultOutputColumnsSupplier)mixeeUserObject).getDefaultOutputColumns(format);
			}
		}
		return _fields;
	}
}


