package com.fortify.cli.sc_sast.rest.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.rest.cli.mixin.AbstractUnirestRunnerWithSessionDataMixin;
import com.fortify.cli.common.rest.runner.GenericUnirestFactory;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_sast.rest.helper.SCSastUnirestHelper;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;

import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

@FixInjection
public abstract class AbstractSCSastUnirestRunnerMixin extends AbstractUnirestRunnerWithSessionDataMixin<SCSastSessionData> {
    @Setter(onMethod=@__({@Inject})) @Getter private SCSastSessionDataManager sessionDataManager;
    @Setter(onMethod=@__({@Inject})) private GenericUnirestFactory unirestFactory;
    
    @Override
    protected final SCSastSessionData getSessionData(String sessionName) {
        return sessionDataManager.get(sessionName, true);
    }
    
    public <R> R runOnSSC(Function<UnirestInstance, R> f) {
        UnirestInstance unirest = unirestFactory.createUnirestInstance();
        SCSastUnirestHelper.configureSscUnirestInstance(unirest, getSessionData());
        return f.apply(unirest);
    }

    public <R> R runOnController(Function<UnirestInstance, R> f) {
        UnirestInstance unirest = unirestFactory.createUnirestInstance();
        SCSastUnirestHelper.configureScSastControllerUnirestInstance(unirest, getSessionData());
        return f.apply(unirest);
    }
}
