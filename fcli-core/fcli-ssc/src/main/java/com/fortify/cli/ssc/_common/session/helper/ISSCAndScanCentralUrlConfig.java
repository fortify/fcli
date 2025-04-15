/**
 * 
 */
package com.fortify.cli.ssc._common.session.helper;

import com.fortify.cli.common.rest.unirest.config.IConnectionConfig;

/**
 * Interface for the functions to get the SSC URL and the Controller URL
 */
public interface ISSCAndScanCentralUrlConfig extends IConnectionConfig {
    String getSscUrl();
    String getScSastControllerUrl();
}
