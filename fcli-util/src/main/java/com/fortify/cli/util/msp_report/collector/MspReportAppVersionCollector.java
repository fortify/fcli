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
package com.fortify.cli.util.msp_report.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.util.Counter;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportProcessingStatus;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppVersionDescriptor;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting and outputting 
 * {@link MspReportSSCProcessedAppVersionDescriptor} instances.</p>
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class MspReportAppVersionCollector {
    private final MspReportResultsWriters writers;
    private final ObjectNode summary;
    
    private Counter totalAppVersionCounter = new Counter();
    private Map<MspReportProcessingStatus, Counter> countsByProcessingStatus = new HashMap<>();
    
    @SneakyThrows
    public void report(IUrlConfig urlConfig, MspReportSSCProcessedAppVersionDescriptor descriptor) {
        totalAppVersionCounter.increase();
        increaseCountByProcessingStatus(descriptor.getStatus());
        writers.appVersionsWriter().write(urlConfig, descriptor);
    }

    void writeResults() {
        writeAppVersionCounts();
    }

    private void writeAppVersionCounts() {
        ObjectNode appVersionCounts = JsonHelper.getObjectMapper().createObjectNode();
        appVersionCounts.put("total", totalAppVersionCounter.getCount());
        Stream.of(MspReportProcessingStatus.values())
            .forEach(status->appVersionCounts.put(status.name(), getCounterByProcessingStatus(status).getCount()));
        summary.set("applicationVersionCounts", appVersionCounts);
    }
    
    private void increaseCountByProcessingStatus(MspReportProcessingStatus status) {
        getCounterByProcessingStatus(status).increase();
    }

    private Counter getCounterByProcessingStatus(MspReportProcessingStatus status) {
        return countsByProcessingStatus.computeIfAbsent(status, x->new Counter());
    }
}
