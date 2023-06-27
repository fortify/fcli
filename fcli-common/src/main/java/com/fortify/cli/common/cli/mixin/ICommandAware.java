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
package com.fortify.cli.common.cli.mixin;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * <p>This interface is to be implemented by mixins that need access to
 * the {@link CommandSpec} of the command that (directly or indirectly
 * through intermediate mixins) references this mixin. Picocli provides
 * the {@link Spec} annotation for injecting the mixee, but this may
 * represent an intermediate mixin rather than the command that indirectly
 * references the mixin.</p>
 * 
 * <p>Mixins usually wouldn't implement this interface directly, but instead
 * utilize {@link CommandHelperMixin} as it provides some useful utility methods
 * related to the the injected {@link CommandSpec}. Injection is handled 
 * by {@link AbstractFortifyCLICommand}.</p>
 * 
 * @author rsenden
 *
 */
public interface ICommandAware {
    void setCommandSpec(CommandSpec commandSpec);
}
