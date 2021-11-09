package com.fortify.cli.common.picocli.component.output;

import java.io.PrintWriter;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.json.transform.jsonpath.JsonPathTransformer;
import com.fortify.cli.common.output.IRecordWriter;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.RecordWriterConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Getter;
import lombok.SneakyThrows;
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
            order=3, defaultValue = "true")
    @Getter
    private boolean withHeaders;

	@CommandLine.Option(names = {"--json-path"}, description = "Transforms output using JSONPath", order = 1)
	@Getter private String jsonPath;
	
	public OutputOptionsWriter getWriter() {
		return new OutputOptionsWriter(getOutputOptionsWriterConfig());
	}
	
	public void write(JsonNode jsonNode) {
		write(writer->writer::write, jsonNode);
	}

	public void write(HttpRequest<?> httpRequest) {
		write(writer->writer::write, httpRequest);
	}
	
	public void write(HttpResponse<JsonNode> httpResponse) {
		write(writer->writer::write, httpResponse);
	}
	
	private <T> void write(Function<OutputOptionsWriter, Consumer<T>> consumer, T input) {
		try ( var writer = getWriter() ) {
			consumer.apply(writer).accept(input);
		}
	}
	
	private OutputOptionsWriterConfig getOutputOptionsWriterConfig() {
		Object mixeeObject = mixee.userObject();
		if ( mixeeObject instanceof IOutputOptionsWriterConfigSupplier ) {
			return ((IOutputOptionsWriterConfigSupplier)mixeeObject).getOutputOptionsWriterConfig();
		} else {
			return new OutputOptionsWriterConfig();
		}
	}
	
	public final class OutputOptionsWriter implements AutoCloseable { // TODO Implement interface, make implementation private
		private final OutputOptionsHandler options = OutputOptionsHandler.this;
		private final OutputOptionsWriterConfig config;
		private final OutputFormat outputFormat;
		private final IRecordWriter recordWriter;
		
		public OutputOptionsWriter(OutputOptionsWriterConfig config) {
			this.config = config;
			this.outputFormat = getOutputFormat();
			this.recordWriter = outputFormat.getRecordWriterFactory().createRecordWriter(createOutputWriterConfig());
		}
		
		public void write(JsonNode jsonNode) {
			jsonNode = config.applyInputTransformations(outputFormat, jsonNode);
			if ( jsonNode.isArray() ) {
				jsonNode.elements().forEachRemaining(this::writeRecord);
			} else if ( jsonNode.isObject() ) {
				writeRecord(jsonNode);
			} else {
				throw new RuntimeException("Not sure what to do here");
			}
		}
		
		public void write(HttpRequest<?> httpRequest) {
			httpRequest.asObject(JsonNode.class)
				.ifSuccess(this::write);
				//TODO .ifFailure(...);
		}
		
		public void write(HttpResponse<JsonNode> httpResponse) {
			write(httpResponse.getBody());
		}

		@SneakyThrows
		private void writeRecord(JsonNode jsonNode) {
			jsonNode = config.applyRecordTransformations(outputFormat, jsonNode); // TODO Before or after other transformations?
			jsonNode = applyJsonPathTransformation(outputFormat, jsonNode);
			jsonNode = config.applyFieldsTransformations(outputFormat, options.fields, jsonNode);
			jsonNode = applyFlattenTransformation(outputFormat, jsonNode);
			if(jsonNode.getNodeType() == JsonNodeType.ARRAY) {
				if(jsonNode.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(jsonNode.get(0).toString()));
			} else {
				recordWriter.writeRecord((ObjectNode) jsonNode);
			}
		}
		
		// TODO Move to OutputOptionsWriterConfig to allow defaults?
		protected JsonNode applyJsonPathTransformation(OutputFormat outputFormat, JsonNode data) {
			if ( StringUtils.isNotEmpty(options.jsonPath) ) {
				data = new JsonPathTransformer(options.jsonPath).transform(data);
			}
			return data;
		}
		
		// TODO Move to OutputOptionsWriterConfig to allow defaults?
		protected JsonNode applyFlattenTransformation(OutputFormat outputFormat, JsonNode data) {
			if ( options.flatten ) {
				data = new FlattenTransformer(outputFormat.getDefaultFieldNameFormatter(), ".", false).transform(data);
			}
			return data;
		}

		private OutputFormat getOutputFormat() {
	    	OutputFormat result = options.outputFormat;
	    	if ( result == null ) {
	    		result = config.defaultFormat();
	    	}
	    	if ( result == null ) {
	    		result = OutputFormat.table;
	    	}
	    	return result;
	    }
		
		private RecordWriterConfig createOutputWriterConfig() {
			return RecordWriterConfig.builder()
					.printWriterSupplier(this::getPrintWriter)
					.headersEnabled(isWithHeaders())
					.build();
		}
		
		private final PrintWriter getPrintWriter() {
			return new PrintWriter(System.out);
		}

		@Override
		public void close() {
			recordWriter.finishOutput();
		}
	}
}


