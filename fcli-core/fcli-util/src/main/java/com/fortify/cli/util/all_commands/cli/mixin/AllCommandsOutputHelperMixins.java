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
package com.fortify.cli.util.all_commands.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins.TableNoQuery;

import picocli.CommandLine.Command;

/**
 *
 * @author Ruud Senden
 */
public class AllCommandsOutputHelperMixins {
    /**
     * Non-queryable list command, as AllCommands commands provide
     * their own query functionality. 
     */
    @Command(name = "list", aliases = {"ls"})
    public static class List extends TableNoQuery {
        public static final String CMD_NAME = "list";
    }
}
