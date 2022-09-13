package com.fortify.cli.ssc.session.manager;

import com.fortify.cli.common.session.manager.spi.AbstractSessionDataManager;

import jakarta.inject.Singleton;

@Singleton
public class SSCSessionDataManager extends AbstractSessionDataManager<SSCSessionData> {
    @Override
    public String getSessionTypeName() {
        return "SSC";
    }

    @Override
    protected Class<SSCSessionData> getSessionDataType() {
        return SSCSessionData.class;
    }
}
