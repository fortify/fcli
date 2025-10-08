/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler.ActionInvalidSchemaVersionHandler;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler.ActionInvalidSignatureHandler;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class describes an fcli action import entry.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonTypeName("import.fcli.action")
@JsonClassDescription("Define an fcli action to be imported for reference in this action.")
@SampleYamlSnippets({"""
        import.fcli.actions:
          pkg1: ssc:package
          pkg2:
            action: ssc:package
            on.unsigned: warn
            on.invalid-signature: fail
            on.invalid-version: warn
        """})
public final class ActionImportFcliActionEntry implements IActionElement, IMapKeyAware<String> {
    @JsonIgnore private String key;
    
    /** Allow for deserializing from a string that specifies the [module:]action format */
    public ActionImportFcliActionEntry(String actionString) {
        this.action = actionString;
    }
    
    @JsonPropertyDescription("""
        Required string: The fcli action to import, optionally qualified with module name. \
        Can be specified as either 'action' (searches in all modules) or 'module:action' \
        for a specific module (e.g., 'ssc:package', 'fod:application').
        """)
    @JsonProperty(value = "action", required = true) 
    private String action;
    
    @JsonPropertyDescription("""
        Optional validation mode: Defines how to handle unsigned actions. Default: ignore
        """)
    @JsonProperty(value = "on.unsigned", required = false) 
    private ActionInvalidSignatureHandler onUnsigned = ActionInvalidSignatureHandler.ignore;
    
    @JsonPropertyDescription("""
        Optional validation mode: Defines how to handle actions with invalid signatures. Default: ignore
        """)
    @JsonProperty(value = "on.invalid-signature", required = false) 
    private ActionInvalidSignatureHandler onInvalidSignature = ActionInvalidSignatureHandler.ignore;
    
    @JsonPropertyDescription("""
        Optional validation mode: Defines how to handle actions with invalid schema versions. Default: ignore
        """)
    @JsonProperty(value = "on.invalid-version", required = false) 
    private ActionInvalidSchemaVersionHandler onInvalidVersion = ActionInvalidSchemaVersionHandler.ignore;
    
    @JsonPropertyDescription("""
        Optional validation mode: Defines how to handle actions that are not found. Default: ignore
        """)
    @JsonProperty(value = "on.not-found", required = false) 
    private ActionInvalidSignatureHandler onNotFound = ActionInvalidSignatureHandler.ignore;
    
    /**
     * Get the module name from the action string, or null if no module is specified.
     * @return module name or null
     */
    @JsonIgnore
    public String getModule() {
        if (action != null && action.contains(":")) {
            return action.substring(0, action.indexOf(":"));
        }
        return null;
    }
    
    /**
     * Get the unqualified action name (without module prefix).
     * @return action name without module prefix
     */
    @JsonIgnore
    public String getActionName() {
        if (action != null && action.contains(":")) {
            return action.substring(action.indexOf(":") + 1);
        }
        return action;
    }
    
    /**
     * Check if this import specifies a module.
     * @return true if module is specified, false otherwise
     */
    @JsonIgnore
    public boolean hasModule() {
        return getModule() != null;
    }
    
    /**
     * Lazy getter that loads and returns the Action instance for this import.
     * Uses the validation handlers configured for this import entry.
     */
    @Getter(lazy=true) private final Action loadedAction = loadAction();
    
    private Action loadAction() {
        var actionSources = hasModule() 
            ? ActionSource.defaultActionSources(getModule())
            : ActionSource.externalActionSources(null);
            
        var validationHandler = ActionValidationHandler.builder()
            .onSignatureStatusDefault(onUnsigned)
            .onUnsupportedSchemaVersion(onInvalidVersion)
            .build();
            
        var loadResult = ActionLoaderHelper.load(actionSources, action, validationHandler);
        return loadResult.getAction();
    }
    
    @Override
    public void postLoad(Action action) {
        if (this.action == null || this.action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action import must specify a valid action");
        }
        // Validate format - should be either "action" or "module:action"
        if (this.action.contains(":")) {
            String[] parts = this.action.split(":", 2);
            if (parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid action format. Use 'action' or 'module:action'");
            }
        }
    }
}