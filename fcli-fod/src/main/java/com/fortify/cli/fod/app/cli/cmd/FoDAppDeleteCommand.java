package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDHttpUpdateCommand;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "delete")
public class FoDAppDeleteCommand extends AbstractFoDHttpUpdateCommand implements IOutputConfigSupplier {

    @Parameters(index = "0", arity = "1", descriptionKey = "appNameOrId")
    private String appNameOrId;

    @SneakyThrows
    @Override
    protected Void run(UnirestInstance unirest) {
        validate();

        // current app being updated
        FoDAppDescriptor appCurrent = FoDAppHelper.getApp(unirest, appNameOrId, true);

        JsonNode response = unirest.delete(FoDUrls.APPLICATION)
                .routeParam("appId", String.valueOf(appCurrent.getApplicationId()))
                .asObject(JsonNode.class).getBody();
        // TODO: return appropriate response as no data returned
        getOutputMixin().write(response);
        return null;
    }

    private void validate() {
        // TODO
    }
}
