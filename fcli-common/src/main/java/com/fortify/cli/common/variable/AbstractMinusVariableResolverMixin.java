package com.fortify.cli.common.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;

import picocli.CommandLine.Model.CommandSpec;

public abstract class AbstractMinusVariableResolverMixin {
    protected String resolveMinusVariable(String value) {
        if ( !"-".equals(value) ) { return value; }
        MinusVariableDefinition def = getMinusVariableDefinition();
        String variableName = FcliVariableHelper.resolveVariableName(getCommand(), def.name());
        String variableField = def.field();
        JsonNode contents = FcliVariableHelper.getVariableContents(variableName, false);
        if ( contents==null ) {
            throw new IllegalStateException(String.format("No previously stored value found for '-' (variable name: %s)", variableName));
        }
        return JsonHelper.evaluateJsonPath(contents, variableField, String.class);
    }

    private Object getCommand() {
        return getCommandSpec().userObject();
    }

    private final CommandSpec getCommandSpec() {
        CommandSpec mixee = getMixee();
        return mixee.commandLine()==null ? mixee : mixee.commandLine().getCommandSpec();
    }
    
    private final MinusVariableDefinition getMinusVariableDefinition() {
        MinusVariableDefinition definition = getMVDClass().getAnnotation(MinusVariableDefinition.class);
        if ( definition==null ) {
            throw new RuntimeException(String.format("%s doesn't provide @MinusVariableDefinition", getMVDClass().getName()));
        }
        return definition;
    }
    
    protected abstract Class<?> getMVDClass();
    protected abstract CommandSpec getMixee();
}
