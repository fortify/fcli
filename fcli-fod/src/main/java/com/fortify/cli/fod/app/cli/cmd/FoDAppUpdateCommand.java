package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.app.mixin.FoDCriticalityTypeOptions;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDHttpUpdateCommand;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.Map;

@ReflectiveAccess
@Command(name = "update")
public class FoDAppUpdateCommand extends AbstractFoDHttpUpdateCommand implements IOutputConfigSupplier {

    @Parameters(index = "0", arity = "1", descriptionKey = "appNameOrId")
    private String appNameOrId;
    @Option(names = {"--name,", "-n"}, descriptionKey = "appName")
    private String applicationNameUpdate;
    @Option(names = {"--description", "-d"}, descriptionKey = "appDesc")
    private String descriptionUpdate;
    // TODO: should we add to existing "emailList", replace or amend? currently if specified "emaliList" is replaced
    @Option(names = {"--notify"}, arity = "0..*", descriptionKey = "")
    private ArrayList<String> notificationsUpdate;

    @Mixin
    private FoDCriticalityTypeOptions.OptionalCritOption criticalityTypeUpdate;
    @Mixin
    private FoDAttributeUpdateOptions.OptionalAttrOption appAttrsUpdate;

    @SneakyThrows
    @Override
    protected Void run(UnirestInstance unirest) {
        validate();

        // current values of app being updated
        FoDAppDescriptor appCurrent = FoDAppHelper.getApp(unirest, appNameOrId, true);
        ArrayList<FoDAttributeDescriptor> appAttrsCurrent = appCurrent.getAttributes();

        // new values to replace
        FoDCriticalityTypeOptions.FoDCriticalityType appCriticalityNew = criticalityTypeUpdate.getCriticalityType();
        Map<String, String> attributeUpdates = appAttrsUpdate.getAttributes();
        JsonNode jsonAttrs = getObjectMapper().createArrayNode();
        if (attributeUpdates != null && attributeUpdates.size() > 0) {
            jsonAttrs = mergeAttributesNode(unirest, appAttrsCurrent, attributeUpdates);
        } else {
            jsonAttrs = getAttributesNode(appAttrsCurrent);
        }
        String appEmailListNew = getEmailList(notificationsUpdate);

        ObjectNode body = getObjectMapper().createObjectNode();
        body.put("applicationName",
                StringUtils.isNotEmpty(applicationNameUpdate) ? applicationNameUpdate : appCurrent.getApplicationName());
        body.put("applicationDescription",
                StringUtils.isNotEmpty(descriptionUpdate) ? descriptionUpdate : appCurrent.getApplicationDescription());
        body.put("businessCriticalityType",
                appCriticalityNew != null ? String.valueOf(appCriticalityNew) : appCurrent.getBusinessCriticalityType());
        body.put("emailList",
                StringUtils.isNotEmpty(appEmailListNew) ? appEmailListNew : appCurrent.getEmailList());
        body.set("attributes", jsonAttrs);

        //System.out.println(body.toPrettyString());

        unirest.put(FoDUrls.APPLICATION)
                .routeParam("appId", String.valueOf(appCurrent.getApplicationId()))
                .body(body).asObject(JsonNode.class).getBody();
        // retrieve the updated application
        JsonNode getResponse = unirest.get(FoDUrls.APPLICATION)
                .routeParam("appId", String.valueOf(appCurrent.getApplicationId()))
                .asObject(JsonNode.class).getBody();
        getOutputMixin().write(getResponse);
        return null;
    }

    private void validate() {
        // TODO
    }

}
