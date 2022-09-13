package com.fortify.cli.sc_sast.session.manager;

import com.fortify.cli.common.session.manager.spi.AbstractSessionDataManager;

import jakarta.inject.Singleton;

@Singleton
public class SCSastSessionDataManager extends AbstractSessionDataManager<SCSastSessionData> {
    @Override
    public String getSessionTypeName() {
        return "SC-SAST";
    }

    @Override
    protected Class<SCSastSessionData> getSessionDataType() {
        return SCSastSessionData.class;
    }
}
