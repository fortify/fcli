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

package com.fortify.cli.fod.scan.helper;

import java.util.ArrayList;
import java.util.stream.Stream;

public enum FoDScanStatus {
    Not_Started, In_Progress, Completed, Canceled, Waiting, Scheduled, Queued;

    public static final FoDScanStatus[] getFailureStates() {
        return new FoDScanStatus[]{ Canceled };
    }

    public static final FoDScanStatus[] getKnownStates() {
        return FoDScanStatus.values();
    }

    public static final FoDScanStatus[] getDefaultCompleteStates() {
        return new FoDScanStatus[]{ Completed };
    }

    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(FoDScanStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(FoDScanStatus::name).toArray(String[]::new);
    }

    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(FoDScanStatus::name).toArray(String[]::new);
    }
    
    public static final class FoDScanStatusIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDScanStatusIterable() { 
            super(Stream.of(FoDScanStatus.values()).map(Enum::name).toList()); 
        }
    }

}
