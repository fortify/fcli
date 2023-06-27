/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.autocomplete.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * This command is responsible for generating bash/zsh completion scripts, allowing users to use the 
 * TAB key to see and auto-complete options and sub-commands.
 * 
 * @author Ruud Senden
 * 
 */
@Command(name = "generate")
public final class AutoCompleteGenerationCommand extends AbstractFortifyCLICommand implements Runnable {
    @Spec CommandSpec spec;

    public void run() {
        String script = AutoComplete.bash(spec.root().name(), spec.root().commandLine());
        // not PrintWriter.println: scripts with Windows line separators fail in strange
        // ways!
        spec.commandLine().getOut().print(script);
        spec.commandLine().getOut().print('\n');
        spec.commandLine().getOut().flush();
    }
}
