/**
 * 
 */
package com.fortify.cli.sc_sast._common.session.helper;

import com.fortify.cli.common.rest.unirest.config.IConnectionConfig;

/**
 * Interface for the functions to get the SSC URL and the Controller URL
 */
public interface ISCSastAndSSCUrlConfig extends IConnectionConfig {
    String getSscUrl();
    String getControllerUrl();
}
