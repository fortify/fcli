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
package com.fortify.cli.fod.report.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.stream.Stream;

public enum FoDReportStatus {
    Started(1), Completed(2), Failed(3), Queued(4);

    private int statusValue;

    FoDReportStatus(int i) {
        this.statusValue = i;
    }

    public int getValue() {
        return statusValue;
    }

    public static FoDReportStatus valueOf(Integer index){
        return FoDReportStatus.values()[index];
    }

    public static JsonNode addReportStatus(JsonNode reportRecord) {
        ObjectNode record = reportRecord==null || !(reportRecord instanceof ObjectNode)
                ? null
                : (ObjectNode)reportRecord;
        if ( record != null ) {
            int reportStatusType = record.get("reportStatusType").asInt();
            return record.put("reportStatus", FoDReportStatus.valueOf(reportStatusType).toString());
        }
        return reportRecord;
    }

    public static final FoDReportStatus[] getFailureStates() {
        return new FoDReportStatus[]{ Failed };
    }

    public static final FoDReportStatus[] getKnownStates() {
        return FoDReportStatus.values();
    }

    public static final FoDReportStatus[] getDefaultCompleteStates() {
        return new FoDReportStatus[]{ Completed };
    }

    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(FoDReportStatus::name).toArray(String[]::new);
    }

    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(FoDReportStatus::name).toArray(String[]::new);
    }

    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(FoDReportStatus::name).toArray(String[]::new);
    }

    public static final class FoDReportStatusIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDReportStatusIterable() {
            super(Stream.of(FoDReportStatus.values()).map(Enum::name).toList());
        }
    }

}
