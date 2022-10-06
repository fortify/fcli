package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.fod.app.mixin.FoDAppTypeMixin;
import com.fortify.cli.fod.app.mixin.FoDCriticalityTypeMixin;
import com.fortify.cli.fod.app.mixin.FoDSdlcStatusTypeMixin;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateMixin;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDHttpUpdateCommand;
import com.fortify.cli.fod.user.helper.FoDUserDescriptor;
import com.fortify.cli.fod.user.helper.FoDUserHelper;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupDescriptor;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ArrayList;
import java.util.Map;

@ReflectiveAccess
@Command(name = "create")
public class FoDApplicationCreateCommand extends AbstractFoDHttpUpdateCommand implements IOutputConfigSupplier {
    @Spec
    CommandSpec spec;

    @Parameters(index = "0", arity = "1", descriptionKey = "appName")
    private String applicationName;
    @Parameters(index = "1", arity = "1", descriptionKey = "relName")
    private String releaseName;
    @Option(names = {"--description", "-d"}, descriptionKey = "appDesc")
    private String description;
    @Option(names = {"--notify", "-nf"}, required = false, arity = "0..*", descriptionKey = "notify")
    private ArrayList<String> notifications;
    @Option(names = {"--release-description", "-rd"}, descriptionKey = "relDesc")
    private String releaseDescription;
    @Option(names = {"--owner", "-own"}, required = true, descriptionKey = "owner")
    private String owner;
    @Option(names = {"--user-group", "-ug"}, arity = "0..*", descriptionKey = "userGroup")
    private ArrayList<String> userGroups;
    @Option(names = {"--microservice", "-ms"}, arity = "0..*", descriptionKey = "microservice")
    private ArrayList<String> microservices;
    @Option(names = {"--release-microservice", "-rms"}, descriptionKey = "releaseMs")
    private String releaseMicroservice;

    @Mixin
    private FoDAppTypeMixin.AppTypeOption typeMixin;
    @Mixin
    private FoDCriticalityTypeMixin.CriticalityTypeOption criticalityMixin;
    @Mixin
    private FoDAttributeUpdateMixin.OptionalAttrOption attrUpdateMixin;
    @Mixin
    private FoDSdlcStatusTypeMixin.SdlcStatusTypeOption sdlcStatusMixin;

    @SneakyThrows
    @Override
    protected Void run(UnirestInstance unirest) {
        validate();

        ObjectNode body = getObjectMapper().createObjectNode();
        body.put("applicationName", applicationName)
                .put("applicationDescription", description == null ? "" : description)
                .put("businessCriticalityType", String.valueOf(criticalityMixin.getCriticalityType()))
                .put("emailList", getEmailList(notifications))
                .put("releaseName", releaseName)
                .put("releaseDescription", releaseDescription == null ? "" : releaseDescription)
                .put("sdlcStatusType", String.valueOf(sdlcStatusMixin.getSdlcStatusType()));

        // look up username if supplied to get owner id
        FoDUserDescriptor userDescriptor = FoDUserHelper.getUser(unirest, owner, true);
        body.put("ownerId", userDescriptor.getId());

        FoDAppTypeMixin.FoDAppType appType = typeMixin.getAppType();
        switch (appType) {
            case Web:
                body.put("applicationType", "Web_Thick_Client");
                body.put("hasMicroservices", false);
                break;
            case Mobile:
                body.put("applicationType", "Mobile");
                body.put("hasMicroservices", false);
                break;
            case Microservice:
                body.put("hasMicroservices", true);
                body.set("microservices", getMicroservicesNode(microservices));
                body.put("releaseMicroserviceName", releaseMicroservice == null ? "" : releaseMicroservice);
                break;
            default:
        }
        body.set("attributes", getAttributesNode(unirest));
        body.set("userGroupIds", getUserGroupsNode(unirest, userGroups));

        //System.out.println(body.toPrettyString());

        JsonNode postResponse = unirest.post(FoDUrls.APPLICATIONS).body(body).asObject(JsonNode.class).getBody();
        // retrieve the updated application
        JsonNode getResponse = unirest.get(FoDUrls.APPLICATION)
                .routeParam("appId", postResponse.get("applicationId").asText())
                .asObject(JsonNode.class).getBody();
        getOutputMixin().write(getResponse);
        return null;
    }

    private void validate() {
        // TODO: if "Web" or "Mobile" type check no microservice options are supplied
        if (typeMixin.getAppType().equals(FoDAppTypeMixin.FoDAppType.Microservice)
                && (missing(microservices) || (releaseMicroservice == null || releaseMicroservice.isEmpty()))) {
            throw new ParameterException(spec.commandLine(),
                    "Missing option: if 'Microservice' type is specified then " +
                            "one or more '-microservice' names need to specified " +
                            "as well as the microservice to create the release for " +
                            "using '--release-microservice");
        }
        // TODO: check "release microservice" is in "microservices" list
    }

    protected JsonNode getUserGroupsNode(UnirestInstance unirest, ArrayList<String> userGroups) {
        ArrayNode userGroupArray = getObjectMapper().createArrayNode();
        if (userGroups == null || userGroups.isEmpty()) return userGroupArray;
        for (String ug : userGroups) {
            FoDUserGroupDescriptor userGroupDescriptor = FoDUserGroupHelper.getUserGroup(unirest, ug, true);
            userGroupArray.add(userGroupDescriptor.getId());
        }
        return userGroupArray;
    }

    private final JsonNode getAttributesNode(UnirestInstance unirest) {
        Map<String, String> attributes = attrUpdateMixin.getAttributes();
        ArrayNode attrArray = getObjectMapper().createArrayNode();
        if (attributes == null || attributes.isEmpty()) return attrArray;
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            ObjectNode attrObj = getObjectMapper().createObjectNode();
            FoDAttributeDescriptor attributeDescriptor = FoDAttributeHelper.getAttribute(unirest, attr.getKey(), true);
            attrObj.put("id", attributeDescriptor.getAttributeId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }
}
