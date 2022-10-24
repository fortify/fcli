package com.fortify.cli.sc_sast.session.manager;

import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.session.manager.api.ISessionData;

public interface ISCSastSessionData extends ISessionData {
    IUrlConfig getSscUrlConfig();
    IUrlConfig getScSastUrlConfig();
    char[] getActiveSSCToken();
    char[] getScSastClientAuthToken();
}