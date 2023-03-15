package com.fortify.cli.common.cli.util;

import java.lang.reflect.AnnotatedElement;

import com.fortify.cli.common.util.StringUtils;

import lombok.Setter;
import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

public class FortifyCLIDefaultValueProvider implements CommandLine.IDefaultValueProvider {
    @Setter private static String envPrefix = "FCLI_DEFAULT";

    @Override
    public String defaultValue(ArgSpec argSpec) {
        String envVarSuffix;
        if (argSpec instanceof OptionSpec) {
            final var optionSpec = (OptionSpec) argSpec;
            envVarSuffix = getEnvVarSuffix(optionSpec.userObject(), optionSpec.longestName().replaceAll("^-+", ""));
        } else {
            envVarSuffix = getEnvVarSuffix(argSpec.userObject(), normalizeParamLabel(argSpec.paramLabel()));
        }
        return resolve(argSpec.command(), envVarSuffix);
    }

    private String resolve(CommandSpec command, String suffix) {
        if ( command!=null && suffix!=null ) {
            var envVarName = getEnvVarName(command, suffix);
            String value = System.getenv(envVarName);
            if ( StringUtils.isNotBlank(value) ) { return value; }
            return resolve(command.parent(), suffix);
        }
        return null;
    }
    
    private final String getEnvVarSuffix(Object userObject, String defaultValue) {
        if ( userObject!=null && userObject instanceof AnnotatedElement ) {
            EnvSuffix envSuffixAnnotation = ((AnnotatedElement)userObject).getAnnotation(EnvSuffix.class);
            if ( envSuffixAnnotation!=null ) { return envSuffixAnnotation.value(); }
        }
        return defaultValue;
    }
    
    private final String getEnvVarName(CommandSpec command, String suffix) {
        String qualifiedCommandName = command.qualifiedName("_");
        String combinedName = String.format("%s_%s", qualifiedCommandName, suffix);
        return combinedName.replace('-', '_')
                .replaceAll("__", "_") // Replace duplicate underscores
                .replaceAll("^_+|_+$", "") // Strip leading and trailing underscores
                .toUpperCase().replaceFirst("FCLI", envPrefix);
    }
    
    private final String normalizeParamLabel(String paramLabel) {
        return paramLabel
                .replaceAll("\\s", "_") // Replace whitespace by underscores
                .replaceAll("[^a-zA-Z\\d-_]", "_") // Replace all special characters by underscore
                .replaceAll("(?<!^)([A-Z][a-z]|(?<=[a-z])[^a-z]|(?<=[A-Z])[0-9_])", "_$1"); // Convert to snake-case without splitting uppercase words; see https://stackoverflow.com/a/73485568
    }
}
