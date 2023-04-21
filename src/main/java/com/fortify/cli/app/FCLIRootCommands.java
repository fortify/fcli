/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.app;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.cli.util.FortifyCLIDefaultValueProvider;
import com.fortify.cli.config._main.cli.cmd.ConfigCommands;
import com.fortify.cli.fod._main.cli.cmd.FoDCommands;
import com.fortify.cli.sc_dast._main.cli.cmd.SCDastCommands;
import com.fortify.cli.sc_sast._main.cli.cmd.SCSastCommands;
import com.fortify.cli.ssc._main.cli.cmd.SSCCommands;
import com.fortify.cli.state._main.cli.cmd.StateCommands;
import com.fortify.cli.tool._main.cli.cmd.ToolCommands;
import com.fortify.cli.util._main.cli.cmd.UtilCommands;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
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
@Singleton
@ReflectiveAccess
@Command(name = "fcli", 
    scope = ScopeType.INHERIT, 
    usageHelpAutoWidth = true,
    sortOptions = false, 
    showAtFileInUsageHelp = false,
    resourceBundle = "com.fortify.cli.common.i18n.FortifyCLIMessages",
    versionProvider = FortifyCLIVersionProvider.class,
    defaultValueProvider = FortifyCLIDefaultValueProvider.class,
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
    // We only want to have the --version option on the top-level fcli command
    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info", scope = ScopeType.LOCAL, order = -1002)
    boolean versionInfoRequested;
}
