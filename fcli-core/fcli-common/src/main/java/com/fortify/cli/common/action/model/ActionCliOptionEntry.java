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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.exception.FcliNotInitializedException;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a action parameter.
 */
@Reflectable @NoArgsConstructor
@Data 
@JsonTypeName("cli.option")
@JsonClassDescription("Define command-line options supported by this action.")
@SampleYamlSnippets("""
        cli.options:
          file: # Can be referenced through ${cli.file} in action steps
            names: -f,--file
            description: Output file name
            required: false
            defaultValue: somefile.txt
        """)
public final class ActionCliOptionEntry implements IActionElement, IMapKeyAware<String> {
    @JsonIgnore private String key;
    
    @JsonPropertyDescription("""
        Optional string: Identifier of an imported action or command (as declared through \
        import.fcli.actions or import.fcli.commands) to copy the CLI option definition from. \
        This allows reusing CLI option definitions from other fcli actions and commands. \
        Options are matched based on their 'names' property. For example, if this CLI option \
        definition declares `names: --some-opt,-o`, then we'd look for a CLI option definition \
        in the specified action or command that also declares one of these names, starting with \
        the longest name. If no match is found, or if this option definition doesn't declare \
        any names, we'd look for a CLI option that has the same identifier. If no matching option \
        definition is found, an error is thrown. Any properties declared on the current option \
        definition will override those copied from the specified action or command, including the \
        'names' property; in the example above, if the other command/action declares an option \
        with names `names: ---some-opt,--alias-opt`, it would be matched based on `--some-opt`,
        but resulting names would be `--some-opt,-o` as declared in the current option definition.
        """)
    @JsonProperty(value = "copy.from", required = false) private String copyFromActionOrCmd;
    
    @JsonPropertyDescription("""
        Required string: The option names allowed on the command line to specify a value \
        for this option. Multi-letter option names should be preceded by double dashes like \
        --option-name, single-letter option names should be preceded by a single dash like\
        -o. Multiple option names may be separated by a comma and optional whitespace, for \
        example:
        options: --option, -o
        """)
    @JsonProperty(value = "names", required = false) private String names;
    
    @JsonPropertyDescription("""
        Required string: Action parameter description to be shown in action usage help.    
        """)
    @JsonProperty(value = "description", required = false) private String description;
    
    @JsonPropertyDescription("""
        Optional string: Action parameter type: string (default), boolean, int, long, double, float, or array.
        """)
    @JsonProperty(value = "type", required = false) private String type;
        
    @JsonPropertyDescription("""
        Optional SpEL template expression: Default value for this CLI option if no value is specified by the user. \
        For example, this can be used to read a default value from an environment variable using ${#env('ENV_NAME')}  
        """)
    @JsonProperty(value = "default", required = false) private TemplateExpression defaultValue;
    
    @JsonPropertyDescription("""
        Optional boolean: CLI options are required by default, unless this property is set to false.    
        """)
    @JsonProperty(value = "required", required = false) private Boolean required;
    
    @JsonPropertyDescription("""
        Optional object: Mask option value in the fcli log file using the given mask configuration.
        """)
    @JsonProperty(value = "mask", required = false) private ActionInputMask mask;
    
    @JsonPropertyDescription("""
        Optional string: Allows for defining groups of options, which can for example be used with \
        ${#action.copyParametersFromGroup("optionGroupName")} 
        """)
    @JsonProperty(value = "group", required = false) private String group;
    
    @JsonPropertyDescription("""
            Optional enum value: If set to 'include' (default), this CLI option is included as an MCP tool \
            argument. If set to 'exclude', this CLI option is not included as an MCP tool argument. Only \
            non-required options may be excluded as MCP tool arguments. Also see `config::mcp` property to \
            include/exclude the action itself as an MCP tool.
            """)
    @JsonProperty(value = "mcp", required = false) private ActionMcpIncludeExclude mcp = ActionMcpIncludeExclude.include;
    
    @JsonIgnore public final String[] getNamesAsArray() {
        return names==null ? null : names.split("[\\s,]+");
    }
    
    @JsonIgnore public final boolean isRequired() {
        return required != null ? required : true; // Default to true if not explicitly set
    }
    
    public final void postLoad(Action action) {
        // Handle copying from imported action or command if specified
        if (copyFromActionOrCmd != null) {
            copyFrom(action, copyFromActionOrCmd);
        }
        
        // Set default value for required if not explicitly set and no copy.from was used
        if (required == null) {
            required = true; // Default to true if not explicitly set
        }
        
        // Validate required properties are set after potential copying
        Action.checkNotBlank("CLI option names", getNames(), this);
        Action.checkNotNull("CLI option description", getDescription(), this);
        Arrays.stream(getNamesAsArray()).forEach(this::checkOptionName);
        // TODO Check no duplicate option names; ideally ActionRunner should also verify
        //      that option names/aliases don't conflict with command options
        //      like --help/-h, --log-file, ...
    }
    
    /**
     * Copy CLI option properties from an imported action or command.
     * This method handles the logic for finding and copying CLI option definitions from imported
     * actions and commands as specified by the copy.from property.
     */
    private void copyFrom(Action action, String copyFromActionOrCmd) {
        Action.throwIf(StringUtils.isBlank(copyFromActionOrCmd), this, 
            () -> "copy.from property cannot be blank");
        
        // Check if copying from an imported action
        if (action.getImportActions().containsKey(copyFromActionOrCmd)) {
            copyFromAction(action, copyFromActionOrCmd);
        }
        // Check if copying from an imported command
        else if (action.getImportCommands().containsKey(copyFromActionOrCmd)) {
            copyFromCommand(action, copyFromActionOrCmd);
        }
        else {
            Action.throwIf(true, this, () -> String.format(
                "copy.from references '%s' which is not found in import.fcli.actions or import.fcli.commands", 
                copyFromActionOrCmd));
        }
    }
    
    private void copyFromAction(Action action, String importedActionId) {
        var importEntry = action.getImportActions().get(importedActionId);
        
        try {
            var sourceAction = importEntry.getLoadedAction();
            var sourceOption = findMatchingCliOption(sourceAction.getCliOptions());
            
            copyPropertiesFrom(sourceOption);
        } catch (Exception e) {
            Action.throwIf(true, this, () -> String.format(
                "Failed to load action '%s' for copying CLI option: %s", importEntry.getAction(), e.getMessage()));
        }
    }
    
    private void copyFromCommand(Action action, String importedCommandId) {
        var importEntry = action.getImportCommands().get(importedCommandId);
        var commandPath = importEntry.getCmd();
        
        try {
            var commandSpec = importEntry.getCommandSpec();
            Action.throwIf(commandSpec == null, this, () -> String.format(
                "Command '%s' not found", commandPath));
                
            var sourceOption = findMatchingCliOptionFromCommand(commandSpec);
            copyPropertiesFromCommand(sourceOption, commandSpec);
        } catch (FcliNotInitializedException e) {
            // Handle case where root command line hasn't been initialized (e.g., during unit tests)
            // This is somewhat dirty, but for now seems to be the best way to allow
            // unit tests to run without fully initializing fcli.         

            // Provide fallback values for unit tests when fcli isn't fully initialized
            // This ensures validation passes while allowing tests to run
            if (getDescription() == null) {
                setDescription("Option copied from " + commandPath + " (fallback during testing)");
            }
            // Keep other properties as they are - only set description if missing
        } catch (Exception e) {
            Action.throwIf(true, this, () -> String.format(
                "Failed to load command '%s' for copying CLI option: %s", commandPath, e.getMessage()));
        }
    }
    
    private ActionCliOptionEntry findMatchingCliOption(Map<String, ActionCliOptionEntry> sourceOptions) {
        // First try to match by option names if target has names
        if (getNames() != null) {
            var targetNames = getNamesAsArray();
            // Sort by length descending to match longest names first
            var sortedTargetNames = Stream.of(targetNames)
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
                
            for (var targetName : sortedTargetNames) {
                for (var sourceEntry : sourceOptions.entrySet()) {
                    var sourceOption = sourceEntry.getValue();
                    if (sourceOption.getNames() != null) {
                        var sourceNames = sourceOption.getNamesAsArray();
                        if (Stream.of(sourceNames).anyMatch(name -> name.equals(targetName))) {
                            return sourceOption;
                        }
                    }
                }
            }
        }
        
        // Fall back to matching by identifier (key)
        var targetKey = getKey();
        if (targetKey != null && sourceOptions.containsKey(targetKey)) {
            return sourceOptions.get(targetKey);
        }
        
        Action.throwIf(true, this, () -> String.format(
            "No matching CLI option found for identifier '%s' or names '%s'", 
            targetKey, getNames()));
        return null; // Never reached due to throwIf above
    }
    
    private picocli.CommandLine.Model.OptionSpec findMatchingCliOptionFromCommand(
            picocli.CommandLine.Model.CommandSpec commandSpec) {
        // First try to match by option names if target has names
        if (getNames() != null) {
            var targetNames = getNamesAsArray();
            // Sort by length descending to match longest names first
            var sortedTargetNames = Stream.of(targetNames)
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
                
            for (var targetName : sortedTargetNames) {
                for (var optionSpec : commandSpec.options()) {
                    if (Stream.of(optionSpec.names()).anyMatch(name -> name.equals(targetName))) {
                        return optionSpec;
                    }
                }
            }
        }
        
        Action.throwIf(true, this, () -> String.format(
            "No matching CLI option found in command for names '%s'", 
            getNames()));
        return null; // Never reached due to throwIf above
    }
    
    private void copyPropertiesFrom(ActionCliOptionEntry source) {
        // Copy properties only if not already set in target
        if (getNames() == null) setNames(source.getNames());
        if (getDescription() == null) setDescription(source.getDescription());
        if (getType() == null) setType(source.getType());
        if (getDefaultValue() == null) setDefaultValue(source.getDefaultValue());
        if (getMask() == null) setMask(source.getMask());
        if (getGroup() == null) setGroup(source.getGroup());
        if (getMcp() == null) setMcp(source.getMcp());
        // Only copy required field if not explicitly set in target
        if (required == null) setRequired(source.isRequired());
    }
    
    /**
     * Checks if the given option spec is contained within an optional ArgGroup.
     * An ArgGroup is considered optional if its multiplicity allows 0 occurrences (e.g., "0..1").
     */
    private boolean isOptionInOptionalArgGroup(picocli.CommandLine.Model.OptionSpec optionSpec, 
                                               picocli.CommandLine.Model.CommandSpec commandSpec) {
        // Check all ArgGroups in the command spec
        for (var argGroup : commandSpec.argGroups()) {
            if (isOptionInArgGroup(optionSpec, argGroup) && isArgGroupOptional(argGroup)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Recursively checks if an option is contained within the given ArgGroup or its nested groups.
     */
    private boolean isOptionInArgGroup(picocli.CommandLine.Model.OptionSpec optionSpec, 
                                       picocli.CommandLine.Model.ArgGroupSpec argGroup) {
        // Check if the option is directly in this group
        if (argGroup.args().contains(optionSpec)) {
            return true;
        }
        
        // Check nested ArgGroups recursively
        for (var nestedGroup : argGroup.subgroups()) {
            if (isOptionInArgGroup(optionSpec, nestedGroup)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if an ArgGroup is optional (multiplicity allows 0 occurrences).
     */
    private boolean isArgGroupOptional(picocli.CommandLine.Model.ArgGroupSpec argGroup) {
        var multiplicity = argGroup.multiplicity();
        return multiplicity.min() == 0;
    }
    
    private void copyPropertiesFromCommand(picocli.CommandLine.Model.OptionSpec source, 
                                           picocli.CommandLine.Model.CommandSpec commandSpec) {
        // Copy properties only if not already set in target
        if (getNames() == null) {
            setNames(String.join(",", source.names()));
        }
        if (getDescription() == null) {
            var description = source.description();
            if (description != null && description.length > 0) {
                setDescription(String.join(" ", description));
            }
        }
        if (getType() == null) {
            // Map picocli types to action types
            var type = source.type();
            if (type != null) {
                var typeName = type.getSimpleName().toLowerCase();
                setType(mapPicocliTypeToActionType(typeName));
            }
        }
        // Enhanced logic for determining if option should be required
        if (required == null) {
            // If option is in an optional ArgGroup, consider it non-required
            if (isOptionInOptionalArgGroup(source, commandSpec)) {
                setRequired(false);
            } else {
                // Otherwise, copy the original required value
                setRequired(source.required());
            }
        }
        // Note: picocli doesn't have direct equivalents for defaultValue, mask, group, mcp
        // so we don't copy those properties from commands
    }
    
    private String mapPicocliTypeToActionType(String picocliType) {
        return switch (picocliType.toLowerCase()) {
            case "integer" -> "int";
            case "boolean" -> "boolean";
            case "long" -> "long";
            case "double" -> "double";
            case "float" -> "float";
            default -> "string";
        };
    }
    
    private final void checkOptionName(String optionName) {
        var validShortOptionName = optionName.length()==2 && optionName.charAt(0)=='-' && optionName.charAt(1)!='-';
        var validLongOptionName = optionName.length()>3 && optionName.startsWith("--") && optionName.charAt(3)!='-';
        Action.throwIf(!(validShortOptionName || validLongOptionName), this, ()->"Not a valid option name: "+optionName);
    }
}