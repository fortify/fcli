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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

/**
 * Detects non-directional setting changes, like engine version or target URL changes. Settings
 * that are missing in either scan summary are ignored, as missing data doesn't indicate a change.
 */
public final class FoDAttributionChangedSettingDetector implements IFoDScanConditionDetector {
    private final FoDAttributionCause cause;
    private final UnaryOperator<String> normalizer;
    private final List<Setting> settings;

    public FoDAttributionChangedSettingDetector(FoDAttributionCause cause, UnaryOperator<String> normalizer, Setting... settings) {
        this.cause = cause;
        this.normalizer = normalizer;
        this.settings = List.of(settings);
    }

    public record Setting(String name, String jsonPointer) {}

    @Override
    public List<FoDScanCondition> detect(FoDAttributionContext context) {
        var evidence = new ArrayList<String>();
        for ( var setting : settings ) {
            var previous = context.getPreviousText(setting.jsonPointer());
            var current = context.getCurrentText(setting.jsonPointer());
            if ( StringUtils.isBlank(previous) || StringUtils.isBlank(current) ) { continue; }
            if ( !StringUtils.equals(normalizer.apply(previous), normalizer.apply(current)) ) {
                evidence.add(String.format("%s: %s -> %s", setting.name(), previous, current));
            }
        }
        return evidence.isEmpty()
                ? List.of()
                : List.of(new FoDScanCondition(cause, FoDAttributionDirection.ANY, evidence));
    }
}
