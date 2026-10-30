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

/**
 * Detects changes of boolean settings that broaden or narrow the scan, like allowing form submissions.
 * If multiple settings changed in opposite directions, the resulting direction is unknown.
 */
public final class FoDAttributionBooleanSettingDetector implements IFoDScanConditionDetector {
    private final FoDAttributionCause cause;
    private final List<Setting> settings;

    public FoDAttributionBooleanSettingDetector(FoDAttributionCause cause, Setting... settings) {
        this.cause = cause;
        this.settings = List.of(settings);
    }

    /** @param trueIsBroader Whether enabling this setting broadens the scan */
    public record Setting(String name, String jsonPointer, boolean trueIsBroader) {}

    @Override
    public List<FoDScanCondition> detect(FoDAttributionContext context) {
        var directions = new ArrayList<FoDAttributionDirection>();
        var evidence = new ArrayList<String>();
        for ( var setting : settings ) {
            var previous = context.getPreviousValue(setting.jsonPointer());
            var current = context.getCurrentValue(setting.jsonPointer());
            if ( !previous.isBoolean() || !current.isBoolean() || previous.asBoolean() == current.asBoolean() ) { continue; }
            directions.add(current.asBoolean() == setting.trueIsBroader()
                    ? FoDAttributionDirection.INCREASE : FoDAttributionDirection.DECREASE);
            evidence.add(String.format("%s: %s -> %s", setting.name(), previous.asBoolean(), current.asBoolean()));
        }
        return directions.isEmpty()
                ? List.of()
                : List.of(new FoDScanCondition(cause, FoDAttributionDirection.combine(directions), evidence));
    }
}
