package com.fortify.cli.common.rest.wait;

public interface IWaitHelperControlProperties {
    WaitUnknownStateRequestedAction getOnUnknownStateRequested();
    WaitUnknownOrFailureStateAction getOnFailureState();
    WaitUnknownOrFailureStateAction getOnUnknownState();
    WaitTimeoutAction getOnTimeout();
    String getIntervalPeriod();
    String getTimeoutPeriod();
}
