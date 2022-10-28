
package com.fortify.cli.sc_sast.scan.helper;

import java.util.stream.Stream;

/**
 * Enum entries were copied from com.fortify.cloud.shared.JobState in cloud-shared-[version].jar.
 *
 */
public enum SCSastControllerScanJobState {
    UNKNOWN, 
    PENDING, 
    QUEUED, 
    RUNNING, 
    CANCELING, 
    CANCELED, 
    COMPLETED, 
    FAILED, 
    FAULTED, 
    TIMEOUT;
    
    public static final SCSastControllerScanJobState[] getFailureStates() {
        return new SCSastControllerScanJobState[]{
            FAILED, FAULTED, TIMEOUT, CANCELING, CANCELED, UNKNOWN // TODO Should we consider UNKNOWN as failure state?
        };
    }
    
    public static final SCSastControllerScanJobState[] getKnownStates() {
        return SCSastControllerScanJobState.values();
    }
    
    public static final SCSastControllerScanJobState[] getDefaultCompleteStates() {
        return new SCSastControllerScanJobState[]{ COMPLETED };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SCSastControllerScanJobState::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SCSastControllerScanJobState::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SCSastControllerScanJobState::name).toArray(String[]::new);
    }

}
