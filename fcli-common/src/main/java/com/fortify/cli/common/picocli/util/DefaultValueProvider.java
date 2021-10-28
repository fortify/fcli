package com.fortify.cli.common.picocli.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.micronaut.core.util.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

public class DefaultValueProvider implements CommandLine.IDefaultValueProvider {

	public static final String ENV_VAR_PREFIX = "FCLI_";

	@Override
	public String defaultValue(ArgSpec argSpec) {
		String defaultValue = null;
		if (argSpec.isOption()) {
			final var optionSpec = (OptionSpec) argSpec;
			final var optionName = getOptionName(optionSpec);
			final var qualifiedOptionNames = getQualifiedOptionNames(argSpec.command(), optionName);
			defaultValue = qualifiedOptionNames.stream()
				.map(this::resolveDefaultValue)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
		}
		return defaultValue;
	}

	private List<String> getQualifiedOptionNames(CommandSpec command, String optionName) {
		final var qualifiedOptionNames = new LinkedList<String>();
		qualifiedOptionNames.add(optionName);
		addQualifiedOptionNames(qualifiedOptionNames, command, optionName);
		return qualifiedOptionNames;
	}

	private void addQualifiedOptionNames(LinkedList<String> qualifiedOptionNames, CommandSpec command, String suffix) {
		if ( command!=null && !"fcli".equals(command.name()) ) {
			var qualifiedOptionName = command.name()+"-"+suffix;
			qualifiedOptionNames.addFirst(qualifiedOptionName);
			addQualifiedOptionNames(qualifiedOptionNames, command.parent(), qualifiedOptionName);
		}	
	}

	private final String getOptionName(OptionSpec optionSpec) {
		var optionName = optionSpec.longestName();
		if (optionName.startsWith("--")) {
			optionName = optionName.substring(2);
		}
		return optionName;
	}

	private String resolveDefaultValue(final String optionName) {
		final var envVarName = convertOptionNameToEnvVarName(optionName);
		return resolveFromEnvironment(envVarName);
	}

	private String convertOptionNameToEnvVarName(final String optionName) {
		final var envName = optionName.toUpperCase(Locale.ROOT).replace('-', '_');
		return ENV_VAR_PREFIX + envName;
	}

	private String resolveFromEnvironment(final String envVarName) {
		final String value = System.getenv(envVarName);
		if (StringUtils.isNotEmpty(value)) {
			return value;
		} else {
			return null;
		}
	}

}
