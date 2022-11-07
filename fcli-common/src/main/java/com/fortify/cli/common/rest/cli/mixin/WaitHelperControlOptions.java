package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.rest.wait.IWaitHelperControlProperties;
import com.fortify.cli.common.rest.wait.WaitTimeoutAction;
import com.fortify.cli.common.rest.wait.WaitUnknownOrFailureStateAction;
import com.fortify.cli.common.rest.wait.WaitUnknownStateRequestedAction;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

@ReflectiveAccess
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
