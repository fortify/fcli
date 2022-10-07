package com.fortify.cli.sc_dast.session.manager;

import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.session.manager.api.ISessionData;

public interface ISCDastSessionData extends ISessionData {
    IUrlConfig getSscUrlConfig();
    IUrlConfig getScDastUrlConfig();
    char[] getActiveToken();
}