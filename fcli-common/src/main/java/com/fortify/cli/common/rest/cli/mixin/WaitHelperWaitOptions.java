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
package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.rest.wait.IWaitHelperWaitDefinition;
import com.fortify.cli.common.rest.wait.IWaitHelperWaitDefinitionSupplier;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public final class WaitHelperWaitOptions implements IWaitHelperWaitDefinitionSupplier {
    @Getter @ArgGroup(exclusive = true, multiplicity = "0..1") 
    private WaitHelperWaitOptionsArgGroup waitDefinition = new WaitHelperWaitOptionsArgGroup();
    
    private static final class WaitHelperWaitOptionsArgGroup implements IWaitHelperWaitDefinition {
        @Option(names = {"--while-all"}, required = true, paramLabel = "<state1>[|<state2>]...")
        @Getter private String whileAll;
        @Option(names = {"--while-any", "--while", "-w"}, required = true, paramLabel = "<state1>[|<state2>]...")
        @Getter private String whileAny;
        @Option(names = {"--until-all", "--until", "-u"}, required = true, paramLabel = "<state1>[|<state2>]...")
        @Getter private String untilAll;
        @Getter @Option(names = {"--until-any"}, required = true, paramLabel = "<state1>[|<state2>]...")
        private String untilAny;
    }
}