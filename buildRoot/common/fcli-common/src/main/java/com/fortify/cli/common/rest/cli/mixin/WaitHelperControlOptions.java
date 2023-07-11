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

import com.fortify.cli.common.rest.wait.IWaitHelperControlProperties;
import com.fortify.cli.common.rest.wait.WaitTimeoutAction;
import com.fortify.cli.common.rest.wait.WaitUnknownOrFailureStateAction;
import com.fortify.cli.common.rest.wait.WaitUnknownStateRequestedAction;

import lombok.Getter;
import picocli.CommandLine.Option;

public class WaitHelperControlOptions implements IWaitHelperControlProperties {
    @Option(names= {"--on-unknown-state-requested"}, defaultValue = "fail")
    @Getter private WaitUnknownStateRequestedAction onUnknownStateRequested;
    @Option(names= {"--on-failure-state"}, defaultValue = "fail")
    @Getter private WaitUnknownOrFailureStateAction onFailureState;
    @Option(names= {"--on-unknown-state"}, defaultValue = "fail")
    @Getter private WaitUnknownOrFailureStateAction onUnknownState;
    @Option(names= {"--on-timeout"}, defaultValue = "fail")
    @Getter private WaitTimeoutAction onTimeout;
    @Option(names= {"--interval", "-i"}, defaultValue = "30s")
    @Getter private String intervalPeriod;
    @Option(names= {"--timeout", "-t"}, defaultValue = "1h")
    @Getter private String timeoutPeriod;
}
