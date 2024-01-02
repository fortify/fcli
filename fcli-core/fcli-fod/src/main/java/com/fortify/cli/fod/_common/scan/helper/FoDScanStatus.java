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

package com.fortify.cli.fod._common.scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.stream.Stream;

public enum FoDScanStatus {
    Not_Started(1), In_Progress(2), Completed(3), Canceled(4), Waiting(5), Scheduled(6), Queued(7);

    private int statusValue;

    FoDScanStatus(int i) {
        this.statusValue = i;
    }

    public int getValue() {
        return statusValue;
    }

    public static FoDScanStatus valueOf(Integer index){
        return FoDScanStatus.values()[index-1];
    }

    public static JsonNode addScanStatus(JsonNode scanRecord) {
        ObjectNode record = scanRecord==null || !(scanRecord instanceof ObjectNode)
                ? null
                : (ObjectNode)scanRecord;
        if ( record != null ) {
            int scanStatusType = record.get("scanStatusType").asInt();
            return record.put("scanStatus", FoDScanStatus.valueOf(scanStatusType).toString());
        }
        return scanRecord;
    }

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
