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
package com.fortify.cli.fod._common.scan.helper.compare;

import java.util.Collection;

/**
 * Set of scan type-specific changes of a single kind, like added files or removed URLs
 *
 * @param itemName Singular name of the changed items, like 'file' or 'URL'
 * @param items Changed items (file paths or URLs as listed in the compare output)
 */
public record FoDScanCompareChangeSet(String itemName, Collection<String> items) {}
