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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class FoDScanPollingDescriptor extends JsonNodeHolder {
    private String ScanId;
    private String OpenSourceScanId;
    private String TenantId;
    private String AnalysisStatusId;
    private String OpenSourceStatusId;
    private String AnalysisStatusTypeValue;
    private String AnalysisStatusReasonId;
    private String AnalysisStatusReason;
    private String AnalysisStatusReasonNotes;
    private String IssueCountCritical;
    private String IssueCountHigh;
    private String IssueCountMedium;
    private String IssueCountLow;
    private String PassFailStatus;
    private String PassFailReasonType;
    private String PauseDetails;
    private String ScanType;
}
