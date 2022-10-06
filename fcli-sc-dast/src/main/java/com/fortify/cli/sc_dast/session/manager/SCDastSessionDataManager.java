package com.fortify.cli.sc_dast.session.manager;

import com.fortify.cli.common.session.manager.spi.AbstractSessionDataManager;

import jakarta.inject.Singleton;

@Singleton
public class SCDastSessionDataManager extends AbstractSessionDataManager<SCDastSessionData> {
    @Override
    public String getSessionTypeName() {
        return "SC-DAST";
    }

    @Override
    protected Class<SCDastSessionData> getSessionDataType() {
        return SCDastSessionData.class;
    }
}
