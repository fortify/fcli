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

package com.fortify.cli.fod.entity.lookup.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;

import picocli.CommandLine;

// TODO 'lookup' isn't really an entity, potentially 'lookup-items' could be considered 
//      an FoD entity, so lookup-items should be the command name, and 'lookup' an alias
//      to allow for less typing.
// TODO Any way to refactor this into more 'concrete' entity commands? Given the number of 
//      possible lookup types, we can't define separate entities for each lookup type, but
//      may we can think of a different approach?
@CommandLine.Command(name = "lookup",
        aliases = {"lookup-items"},
        subcommands = {
                FoDLookupListCommand.class
        }
)
@DefaultVariablePropertyName("text")
public class FoDLookupCommands extends AbstractFortifyCLICommand {
}
