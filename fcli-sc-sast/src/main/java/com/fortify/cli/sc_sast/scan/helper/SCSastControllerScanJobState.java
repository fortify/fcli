
package com.fortify.cli.sc_sast.scan.helper;

import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum entries were copied from com.fortify.cloud.shared.JobState in cloud-shared-[version].jar.
 * We keep the _isTerminal property to allow for easy copying of updated enum entries from future
 * product versions, but won't use it. 
 *
 */
@RequiredArgsConstructor
public enum SCSastControllerScanJobState {
    UNKNOWN(false), 
    PENDING(false), 
    QUEUED(false), 
    RUNNING(false), 
    CANCELING(false), 
    CANCELED(true), 
    COMPLETED(true), 
    FAILED(true), 
    FAULTED(true), 
    TIMEOUT(true);
    
    @Getter(AccessLevel.PRIVATE) private final boolean _isTerminal;
    
    public static final SCSastControllerScanJobState[] getFailureStates() {
        return new SCSastControllerScanJobState[]{
            FAILED, FAULTED, TIMEOUT, UNKNOWN // TODO Should we consider UNKNOWN as failure state?
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
