package com.fortify.cli.common.variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;

public abstract class AbstractPredefinedVariableResolverMixin {
    protected String resolvePredefinedVariable(String value) {
        if ( !FcliVariableHelper.PREDEFINED_VARIABLE_PLACEHOLDER.equals(value) ) { return value; }
        PredefinedVariable predefinedVariable = getPredefinedVariable();
        String variableName = predefinedVariable.name();
        String variableField = predefinedVariable.field();
        JsonNode contents = FcliVariableHelper.getVariableContents(variableName, false);
        if ( contents==null ) {
            throw new IllegalStateException(String.format("No previously stored value found for '%s' (variable name: %s)", FcliVariableHelper.PREDEFINED_VARIABLE_PLACEHOLDER, variableName));
        }
        return JsonHelper.evaluateJsonPath(contents, variableField, String.class);
    }
    
    private final PredefinedVariable getPredefinedVariable() {
        PredefinedVariable definition = getPredefinedVariableClass().getAnnotation(PredefinedVariable.class);
        if ( definition==null ) {
            throw new RuntimeException(String.format("%s doesn't provide @%s", getPredefinedVariableClass().getName(), PredefinedVariable.class.getSimpleName()));
        }
        return definition;
    }
    
    protected abstract Class<?> getPredefinedVariableClass();
}
