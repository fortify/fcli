/**
 * 
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
