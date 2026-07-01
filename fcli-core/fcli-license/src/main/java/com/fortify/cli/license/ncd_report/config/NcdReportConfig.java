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
package com.fortify.cli.license.ncd_report.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.report.config.IReportSourceSupplierConfig;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level configuration class defining report sources and contributor settings.
 * 
 * @author rsenden
 *
 */
@Reflectable @NoArgsConstructor 
@Data
public class NcdReportConfig implements IReportSourceSupplierConfig<NcdReportContext> {
    private NcdReportSourcesConfig sources;
    private Optional<NcdReportContributorConfig> contributor;
    /** Not a YAML config field — set programmatically from the {@code --end-date} CLI option. */
    @JsonIgnore private OffsetDateTime commitEndDate;
    
    @Override
    public final Collection<INcdReportSourceConfig> getSourceConfigs() {
        return sources == null ? java.util.List.of() : sources.getSourceConfigs();
    }
    
    /**
     * Returns the (inclusive) end of the 90-day reporting window.
     * When {@code --end-date} is supplied this is end-of-day on that date; otherwise it is now.
     */
    public final OffsetDateTime getCommitEndDateTime() {
        return commitEndDate != null ? commitEndDate : OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Returns the (inclusive) start of the 90-day reporting window: exactly 90 days before
     * {@link #getCommitEndDateTime()}.
     */
    public final OffsetDateTime getCommitStartDateTime() {
        return getCommitEndDateTime().minusDays(90);
    }
}
