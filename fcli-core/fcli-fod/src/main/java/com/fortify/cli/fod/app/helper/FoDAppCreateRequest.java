/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.app.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.access_control.helper.FoDUserGroupHelper;
import com.fortify.cli.fod.access_control.helper.FoDUserHelper;
import com.fortify.cli.fod.app.attr.helper.FoDAttributeHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions.FoDAppType;
import com.fortify.cli.fod.app.cli.mixin.FoDCriticalityTypeOptions.FoDCriticalityType;
import com.fortify.cli.fod.app.cli.mixin.FoDSdlcStatusTypeOptions.FoDSdlcStatusType;
import com.fortify.cli.fod.release.helper.FoDQualifiedReleaseNameDescriptor;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Getter
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FoDAppCreateRequest {
    private String applicationName;
    private String applicationDescription;
    private String businessCriticalityType;
    private String emailList;
    private String releaseName;
    private String releaseDescription;
    private String sdlcStatusType;
    private Integer ownerId;
    private String applicationType;
    @Builder.Default private boolean hasMicroservices = false;
    private JsonNode microservices;
    private String releaseMicroserviceName;
    private JsonNode attributes;
    private JsonNode userGroupIds;
    
    @JsonIgnore
    public final ObjectNode asObjectNode() {
        ObjectNode body = JsonHelper.getObjectMapper().valueToTree(this);
        // if microservice, remove applicationType field
        if (isHasMicroservices()) {
            body.remove("applicationType");
        }
        return body;
    }
    
    @JsonIgnore
    public final FoDAppCreateRequest validate(Consumer<List<String>> validationMessageConsumer) {
        var messages = new ArrayList<String>();
        validateRequired(messages, applicationName, "Required application name not specified");
        validateRequired(messages, businessCriticalityType, "Required application business criticality not specified");
        validateRequired(messages, releaseName, "Required release name not specified");
        validateRequired(messages, sdlcStatusType, "Required release SDLC status not specified");
        validateRequired(messages, applicationType, "Required application type not specified");
        if ( hasMicroservices ) {
            validateRequired(messages, releaseMicroserviceName, "Required release microservice name not specified");
        }
        if ( !messages.isEmpty() ) {
            validationMessageConsumer.accept(messages);
        }
        return this;
    }
    
    @JsonIgnore
    public final FoDAppCreateRequest validate() {
        return validate(messages->{throw new FcliSimpleException("Unable to create application:\n\t"+String.join("\n\t", messages)); });
    }
    
    @JsonIgnore
    private final void validateRequired(List<String> messages, Object obj, String message) {
        if ( obj==null || (obj instanceof String && StringUtils.isBlank((String)obj)) ) {
            messages.add(message);
        }
    }
    
    public static class FoDAppCreateRequestBuilder {
        public FoDAppCreateRequestBuilder appType(FoDAppType appType) {
            if ( appType==null ) { return hasMicroservices(false).applicationType(null); }
            else { return hasMicroservices(appType.isMicroservice()).applicationType(appType.getFoDValue()); }
        }
        
        public FoDAppCreateRequestBuilder autoAttributes(UnirestInstance unirest, Map<String, String> attributes, boolean autoRequiredAttrs) {
            return attributes(FoDAttributeHelper.getAttributesNode(unirest, FoDEnums.AttributeTypes.All, attributes, autoRequiredAttrs));
        }
        
        public FoDAppCreateRequestBuilder businessCriticality(FoDCriticalityType businessCriticalityType) {
            return businessCriticalityType(businessCriticalityType==null ? null : businessCriticalityType.name());
        }
        
        public FoDAppCreateRequestBuilder notify(ArrayList<String> notifications) {
            return emailList(FoDAppHelper.getEmailList(notifications));
        }
        
        public FoDAppCreateRequestBuilder microserviceName(String microserviceName) {
            List<String> microservices = StringUtils.isBlank(microserviceName)
                    ? Collections.emptyList() : new ArrayList<>(Arrays.asList(microserviceName));
            microservices(FoDAppHelper.getMicroservicesNode(microservices));
            return releaseMicroserviceName(microserviceName);
        }
        
        public FoDAppCreateRequestBuilder microserviceAndReleaseNameDescriptor(FoDMicroserviceAndReleaseNameDescriptor microserviceAndReleaseNameDescriptor) {
            microserviceName(microserviceAndReleaseNameDescriptor==null ? null : microserviceAndReleaseNameDescriptor.getMicroserviceName());
            return releaseName(microserviceAndReleaseNameDescriptor==null ? null : microserviceAndReleaseNameDescriptor.getReleaseName());
        }
        
        public FoDAppCreateRequestBuilder releaseNameDescriptor(FoDQualifiedReleaseNameDescriptor releaseNameDescriptor) {
            applicationName(releaseNameDescriptor==null ? null : releaseNameDescriptor.getAppName());
            microserviceName(releaseNameDescriptor==null ? null : releaseNameDescriptor.getMicroserviceName());
            return releaseName(releaseNameDescriptor==null ? null : releaseNameDescriptor.getReleaseName());
        }
        
        public FoDAppCreateRequestBuilder owner(UnirestInstance unirest, String owner) {
            int userId = 0;
            if (owner == null) return ownerId(null);
            try {
                userId = Integer.parseInt(owner);
            } catch (NumberFormatException nfe) {
                userId = FoDUserHelper.getUserDescriptor(unirest, owner, true).getUserId();
            }
            return ownerId(userId);
        }
        
        public FoDAppCreateRequestBuilder sdlcStatus(FoDSdlcStatusType sdlcStatusType) {
            return sdlcStatusType(sdlcStatusType==null ? null : sdlcStatusType.name());
        }
        
        public FoDAppCreateRequestBuilder userGroups(UnirestInstance unirest, ArrayList<String> userGroups) {
            return userGroupIds(FoDUserGroupHelper.getUserGroupsNode(unirest, userGroups));
        }
    }
}
