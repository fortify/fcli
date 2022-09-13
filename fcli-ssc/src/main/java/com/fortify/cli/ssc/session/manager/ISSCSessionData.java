package com.fortify.cli.ssc.session.manager;

import com.fortify.cli.common.session.manager.api.ISessionData;

public interface ISSCSessionData extends ISessionData {
    char[] getActiveToken();
}