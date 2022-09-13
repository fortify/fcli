package com.fortify.cli.fod.session.manager;

import com.fortify.cli.common.session.manager.spi.AbstractSessionDataManager;

import jakarta.inject.Singleton;

@Singleton
public class FoDSessionDataManager extends AbstractSessionDataManager<FoDSessionData> {
    @Override
    public String getSessionTypeName() {
        return "FoD";
    }

    @Override
    protected Class<FoDSessionData> getSessionDataType() {
        return FoDSessionData.class;
    }
}
