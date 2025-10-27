/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.sc_sast.scan.helper;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Enum entries were copied from com.fortify.cloud.shared.JobState in cloud-shared-[version].jar.
 *
 */
public enum SCSastScanJobState {
    UNKNOWN, 
    PENDING, 
    QUEUED, 
    RUNNING, 
    CANCELING, 
    CANCELED, 
    COMPLETED, 
    FAILED, 
    FAULTED, 
    TIMEOUT,
    
    // Publish disabled state
    NO_PUBLISH
    ;
    
    public static final SCSastScanJobState[] getFailureStates() {
        return new SCSastScanJobState[]{
            FAILED, FAULTED, TIMEOUT, CANCELING, CANCELED, UNKNOWN // TODO Should we consider UNKNOWN as failure state?
        };
    }
    
    public static final SCSastScanJobState[] getKnownStates() {
        return SCSastScanJobState.values();
    }
    
    public static final SCSastScanJobState[] getDefaultCompleteStates() {
        return new SCSastScanJobState[]{ COMPLETED };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SCSastScanJobState::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SCSastScanJobState::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SCSastScanJobState::name).toArray(String[]::new);
    }
    
    public static final class SCSastControllerScanJobStateIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public SCSastControllerScanJobStateIterable() { 
            super(Stream.of(SCSastScanJobState.values()).map(Enum::name).toList()); 
        }
    }

}
