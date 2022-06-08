package com.fortify.cli.common.picocli.mixin.output;

import java.util.function.Function;

import com.fortify.cli.common.output.OutputFormat;

public interface IDefaultFieldNameFormatterProvider {
	public Function<String, String> getDefaultFieldNameFormatter(OutputFormat outputFormat);
}
