/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.scan.helper;

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

}
