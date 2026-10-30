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

/** Index of scan type-specific changes, like scan manifest file changes or site tree URL changes */
public interface IFoDAttributionChangeIndex {
    /** @return true if the given issue location is known to have been added, removed, moved or changed */
    boolean isLocationChanged(String location);
}
