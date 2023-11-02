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

package com.fortify.cli.ssc.system_state.helper;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Enum values copied from SSC API Reference enum
 */
public enum SSCJobStatus {
    PREPARED, FINISHED, RUNNING, DEFERRED, FAILED, CANCELLED, CANCELLING;

    public static final SSCJobStatus[] getFailureStates() {
        return new SSCJobStatus[]{
                DEFERRED, FAILED, CANCELLED
        };
    }

    public static final SSCJobStatus[] getKnownStates() {
        return SSCJobStatus.values();
    }

    public static final SSCJobStatus[] getDefaultCompleteStates() {
        return new SSCJobStatus[]{FINISHED};
    }

    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SSCJobStatus::name).toArray(String[]::new);
    }

    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SSCJobStatus::name).toArray(String[]::new);
    }

    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SSCJobStatus::name).toArray(String[]::new);
    }

    public static final class SSCJobStatusIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public SSCJobStatusIterable() {
            super(Stream.of(SSCJobStatus.values()).map(SSCJobStatus::name).toList());
        }
    }

}
