/**
 * This package defines the 'config' top-level command, some generic sub-commands, and any accompanying 
 * classes like mixins and (abstract) base classes (at the time of writing there are no accompanying classes).
 * Any module can add `config` sub-commands by annotating their configuration sub-command with 
 * {@code @SubcommandOf(RootConfigCommand.class)}. At the moment there's only a small number of configuration
 * commands, we could consider using some command hierarchy if we think the list of direct sub-commands
 * grows too large.
 */
package com.fortify.cli.common.picocli.command.config;

