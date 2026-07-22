/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.aviator.ssc.cli.mixin;

import java.time.OffsetDateTime;

import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsSelectorArgGroups.OnlineSelectionArgGroup;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;

@Getter
public class AviatorSSCRemediationsCacheDownloadSelectorMixin {
    @ArgGroup(exclusive = false, multiplicity = "1")
    private OnlineSelectionArgGroup onlineSelection;

    public boolean isArtifactIdSelected() {
        return onlineSelection != null && onlineSelection.isArtifactIdSelected();
    }

    public boolean isLatestSelected() {
        return onlineSelection != null && onlineSelection.isLatestSelected();
    }

    public boolean isAllSelected() {
        return onlineSelection != null && onlineSelection.isAllSelected();
    }

    public String getArtifactId() {
        return onlineSelection != null ? onlineSelection.getArtifactId() : null;
    }

    public String getSince() {
        return onlineSelection != null ? onlineSelection.getSince() : null;
    }

    public String getAppVersionId(UnirestInstance unirest) {
        return onlineSelection != null ? onlineSelection.getAppVersionId(unirest) : null;
    }

    public String getSelectionMode() {
        return onlineSelection != null ? onlineSelection.getSelectionMode() : null;
    }

    public OnlineSelectionArgGroup.ResolvedOnlineArtifacts resolveArtifacts(
            UnirestInstance unirest, OffsetDateTime sinceDate) {
        return onlineSelection.resolveArtifacts(unirest, sinceDate);
    }

    public void validate() {
        if (onlineSelection != null) {
            onlineSelection.validate();
        }
    }
}
