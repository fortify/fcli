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
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;

/**
 * This class describes an fcli command import entry.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonTypeName("import.fcli.command")
@JsonClassDescription("Define an fcli command to be imported for later reference in this action.")
@SampleYamlSnippets({"""
        import.fcli.commands:
          avList: fcli ssc av ls
          appList:
            cmd: fcli ssc app ls
        """})
public final class ActionImportFcliCommandEntry implements IActionElement, IMapKeyAware<String> {
    @JsonIgnore private String key;
    
    /** Allow for deserializing from a string that specifies the command */
    public ActionImportFcliCommandEntry(String command) {
        this.cmd = command;
    }
    
    @JsonPropertyDescription("""
        Required string: The fcli command to import, including the 'fcli' prefix and all subcommands.
        """)
    @JsonProperty(value = "cmd", required = true) 
    private String cmd;
    
    @Getter(lazy=true) private final CommandSpec commandSpec = FcliCommandSpecHelper.getCommandSpec(getCmd());
    
    @Override
    public void postLoad(Action action) {
        // Validation can be added here if needed
        if (cmd == null || cmd.trim().isEmpty()) {
            throw new IllegalArgumentException("Command import must specify a valid cmd");
        }
    }
}