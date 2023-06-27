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

package com.fortify.cli.sc_sast.entity.scan.helper;

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
