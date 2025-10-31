/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.ssc._common.session.helper;

import java.util.Set;

import com.fortify.cli.common.rest.unirest.config.IConnectionConfig;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCAndScanCentralSessionLoginOptions.SSCAndScanCentralUrlConfigOptions.SSCComponentDisable;

/**
 * Interface for the functions to get the SSC URL and the Controller URL
 */
public interface ISSCAndScanCentralUrlConfig extends IConnectionConfig {
    String getSscUrl();
    String getScSastControllerUrl();
    Set<SSCComponentDisable> getDisabledComponents();
}
