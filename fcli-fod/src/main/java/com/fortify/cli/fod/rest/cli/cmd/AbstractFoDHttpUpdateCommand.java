package com.fortify.cli.fod.rest.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.attribute.helper.FoDAttributeHelper;
import com.fortify.cli.fod.util.FoDOutputConfigHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This base class for FoD update {@link Command} implementations provides capabilities for running
 * FoD POST/PUT/DELETE requests and outputting the result in table format.
 *
 * @author kadraman
 */
@ReflectiveAccess @FixInjection
public abstract class AbstractFoDHttpUpdateCommand extends AbstractFoDUnirestRunnerCommand implements IOutputConfigSupplier {
    @Getter @Mixin private OutputMixinWithQuery outputMixin;
    @Getter private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Void run(UnirestInstance unirest) {
        throw new IllegalStateException("run method must be implemented by subclass");
    }
    
    protected HttpRequest<?> generateRequest(UnirestInstance unirest) {
        throw new IllegalStateException("Either generateRequest or generateOutput method must be implemented by subclass");
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return FoDOutputConfigHelper.table().recordTransformer(this::transformRecord);
    }
    
    protected JsonNode transformRecord(JsonNode record) {
        return record;
    }

    //

    protected String getEmailList(ArrayList<String> notifications) {
        if (notifications != null && !notifications.isEmpty()) {
            return String.join(",", notifications);
        } else {
            return "";
        }
    }

    protected JsonNode getMicroservicesNode(ArrayList<String> microservices) {
        ArrayNode microserviceArray = objectMapper.createArrayNode();
        if (microservices == null || microservices.isEmpty()) return microserviceArray;
        for (String ms : microservices) {
            microserviceArray.add(ms);
        }
        return microserviceArray;
    }

    protected JsonNode getUserGroupsNode(ArrayList<Integer> userGroups) {
        ArrayNode userGroupArray = objectMapper.createArrayNode();
        if (userGroups == null || userGroups.isEmpty()) return userGroupArray;
        for (Integer ug : userGroups) {
            userGroupArray.add(ug);
        }
        return userGroupArray;
    }

    protected JsonNode mergeAttributesNode(UnirestInstance unirest,
                                               ArrayList<FoDAttributeDescriptor> current,
                                               Map<String, String> updates) {
        ArrayNode attrArray = objectMapper.createArrayNode();
        if (updates == null || updates.isEmpty()) return attrArray;
        Map<Integer, String> updatesWithId = new HashMap<>();
        for (Map.Entry<String, String> attr : updates.entrySet()) {
            FoDAttributeDescriptor attributeDescriptor = FoDAttributeHelper.getAttribute(unirest, attr.getKey(), true);
            updatesWithId.put(Integer.valueOf(attributeDescriptor.getAttributeId()), attr.getValue());
        }
        for (FoDAttributeDescriptor attr : current) {
            ObjectNode attrObj = objectMapper.createObjectNode();
            attrObj.put("id", attr.getAttributeId());
            if (updatesWithId.containsKey(Integer.valueOf(attr.getAttributeId()))) {
                attrObj.put("value", updatesWithId.get(Integer.valueOf(attr.getAttributeId())));
            } else {
                attrObj.put("value", attr.getValue());
            }
            attrArray.add(attrObj);
        }
        return attrArray;
    }

    protected JsonNode getAttributesNode(ArrayList<FoDAttributeDescriptor> attributes) {
        ArrayNode attrArray = objectMapper.createArrayNode();
        if (attributes == null || attributes.isEmpty()) return attrArray;
        for (FoDAttributeDescriptor attr : attributes) {
            ObjectNode attrObj = objectMapper.createObjectNode();
            attrObj.put("id", attr.getAttributeId());
            attrObj.put("value", attr.getValue());
            attrArray.add(attrObj);
        }
        return attrArray;
    }

    protected boolean missing(List<?> list) {
        return list == null || list.isEmpty();
    }

}