package com.fortify.cli.common.session.manager.spi;

import java.util.Collection;
import java.util.List;

import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.api.SessionSummary;

public interface ISessionDataManager<T extends ISessionData> {
    T get(String sessionName, boolean failIfUnavailable);
    void save(String sessionName, T sessionData);
    void destroy(String sessionName);
    boolean exists(String sessionName);
    List<String> sessionNames();
    Collection<SessionSummary> sessionSummaries();
    String getSessionTypeName();
    void writeSessionSummaries(OutputMixin outputMixin);
}