package com.fortify.cli.common.output.cli;

import java.util.function.Function;

import com.fortify.cli.common.output.writer.OutputFormat;

public interface IDefaultFieldNameFormatterProvider {
	public Function<String, String> getDefaultFieldNameFormatter(OutputFormat outputFormat);
}
