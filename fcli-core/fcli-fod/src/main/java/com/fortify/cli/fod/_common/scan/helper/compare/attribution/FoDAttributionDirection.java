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

import java.util.Collection;

/**
 * Direction of a scan condition, from the perspective of the scan's ability to find issues:
 * <ul>
 *  <li>ANY: non-directional change, like an engine version or target URL change</li>
 *  <li>INCREASE: broader scan or more coverage, including recovery from degradation in the previous scan</li>
 *  <li>DECREASE: narrower scan, less coverage, or degradation in the current scan</li>
 *  <li>CHANGED: changed, but direction can't be determined, for example mixed signals or unranked values</li>
 * </ul>
 */
public enum FoDAttributionDirection {
    ANY, INCREASE, DECREASE, CHANGED;

    public FoDAttributionDirection invert() {
        return switch ( this ) {
            case INCREASE -> DECREASE;
            case DECREASE -> INCREASE;
            default -> this;
        };
    }

    /** Return the common direction if all given directions are the same, or CHANGED otherwise */
    public static FoDAttributionDirection combine(Collection<FoDAttributionDirection> directions) {
        var distinct = directions.stream().distinct().toList();
        return distinct.size() == 1 ? distinct.get(0) : CHANGED;
    }
}
