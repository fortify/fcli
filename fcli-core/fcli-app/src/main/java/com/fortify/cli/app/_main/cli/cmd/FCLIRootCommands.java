/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.app._main.cli.cmd;

import com.fortify.cli.app.FortifyCLIVersionProvider;
import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.config._main.cli.cmd.ConfigCommands;
import com.fortify.cli.fod._main.cli.cmd.FoDCommands;
import com.fortify.cli.sc_dast._main.cli.cmd.SCDastCommands;
import com.fortify.cli.sc_sast._main.cli.cmd.SCSastCommands;
import com.fortify.cli.ssc._main.cli.cmd.SSCCommands;
import com.fortify.cli.state._main.cli.cmd.StateCommands;
import com.fortify.cli.tool._main.cli.cmd.ToolCommands;
import com.fortify.cli.util._main.cli.cmd.UtilCommands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * This is the root command for the fcli application, defining common properties
 * like help options mixin and help footer that will be inherited by all
 * sub-commands. Other than that, it also includes the {@link LoggingMixin}
 * to avoid picocli from complaining about not recognizing those options.
 * Actual logging setup will already have been completed before this command
 * is even loaded, so the logging options themselves are not being processed here. 
 * 
 * @author Ruud Senden
 */
@Command(name = "fcli", 
    resourceBundle = "com.fortify.cli.common.i18n.FortifyCLIMessages",
    versionProvider = FortifyCLIVersionProvider.class,
    subcommands = {
            ConfigCommands.class,
            StateCommands.class,
            FoDCommands.class,
            SCDastCommands.class,
            SCSastCommands.class,
            SSCCommands.class,
            ToolCommands.class,
            UtilCommands.class
    }
)
public class FCLIRootCommands extends AbstractFortifyCLICommand {    
    // We only want to have the --version option on the top-level fcli command,
    @Option(names = {"-V", "--version"}, versionHelp = true, scope = ScopeType.LOCAL, order = -1002)
    @DisableTest(TestType.OPT_SHORT_NAME)
    boolean versionInfoRequested;
}
