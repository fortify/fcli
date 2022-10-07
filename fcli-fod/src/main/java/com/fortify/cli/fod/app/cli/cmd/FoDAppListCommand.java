package com.fortify.cli.fod.app.cli.cmd;

import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDHttpListCommand;
import com.fortify.cli.fod.rest.query.FoDFiltersParamValueGenerators;
import com.fortify.cli.fod.rest.query.FoDOutputQueryFiltersParamGenerator;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine;

@ReflectiveAccess
@CommandLine.Command(name = "list")
public class FoDAppListCommand extends AbstractFoDHttpListCommand {

    @Override
    protected FoDOutputQueryFiltersParamGenerator getFiltersParamGenerator() {
        return new FoDOutputQueryFiltersParamGenerator()
                .add("applicationId", FoDFiltersParamValueGenerators::plain)
                .add("applicationName", FoDFiltersParamValueGenerators::plain)
                .add("businessCriticalityType", FoDFiltersParamValueGenerators::plain)
                .add("applicationType", FoDFiltersParamValueGenerators::plain);
    }

    @Override
    protected GetRequest generateRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.APPLICATIONS);
    }

}
