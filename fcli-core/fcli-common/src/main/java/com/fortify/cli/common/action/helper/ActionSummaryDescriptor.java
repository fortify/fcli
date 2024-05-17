/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.helper;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionLoadResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

@Reflectable @Data @Builder @EqualsAndHashCode(callSuper = false)
public class ActionSummaryDescriptor extends JsonNodeHolder {
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    private final String name;
    private final String status;
    private final boolean custom;
    private final String origin;
    private final String author;
    private final String usageHeader;
    private final String usageDescription;
    private final String signatureStatus;
    private final String signedBy;
    private final String publicKeyName;
    private final String publicKeyFingerprint;
    private final ObjectNode signatureExtraInfo;
    
    public static final ActionSummaryDescriptor fromActionLoadResult(ActionLoadResult actionLoadResult) {
        var metadata = actionLoadResult.getMetadata();
        var builder = ActionSummaryDescriptor.builder();
        builder
            .name(metadata.getName())
            .status(getStatus(actionLoadResult))
            .custom(metadata.isCustom())
            .origin(metadata.isCustom()?"CUSTOM":"FCLI");
        addSignatureInfo(builder, actionLoadResult);
        addActionProperties(builder, actionLoadResult);
        return builder.build();
    }
    
    private static final void addSignatureInfo(ActionSummaryDescriptorBuilder builder, ActionLoadResult actionLoadResult) {
        var metadata = actionLoadResult.getMetadata();
        var signatureStatus = metadata.getSignatureStatus();
        var signatureDescriptor = metadata.getSignatureDescriptor();
        var signatureMetadata = signatureDescriptor==null?null:signatureDescriptor.getMetadata();
        var publicKeyDescriptor = metadata.getPublicKeyDescriptor();
        var signer = StringUtils.defaultIfBlank(signatureMetadata==null 
                ? null : signatureMetadata.getSigner(), "N/A");
        var extraInfo = signatureMetadata==null 
                ? null : signatureMetadata.getExtraInfo();
        var publicKeyName = StringUtils.defaultIfBlank(publicKeyDescriptor==null 
                ? null : publicKeyDescriptor.getName(), "N/A");
        var publicKeyFingerprint = StringUtils.defaultIfBlank(publicKeyDescriptor==null
                ? null : publicKeyDescriptor.getFingerprint(), "N/A");
        
        builder
            .signatureStatus(signatureStatus.toString())
            .signedBy(signer)
            .publicKeyName(publicKeyName)
            .publicKeyFingerprint(publicKeyFingerprint)
            .signatureExtraInfo(extraInfo);
    }

    @SneakyThrows
    private static final void addActionProperties(ActionSummaryDescriptorBuilder builder, ActionLoadResult actionLoadResult) {
        var actionNode = getActionObjectNode(actionLoadResult);
        if ( actionNode==null ) {
            builder
                .author("N/A")
                .usageHeader("N/A")
                .usageDescription("N/A");
        } else {
            builder
                .author(JsonHelper.evaluateSpelExpression(
                        actionNode, "author?:'N/A'", String.class))
                .usageHeader(JsonHelper.evaluateSpelExpression(
                        actionNode, "usage?.header?:'N/A'", String.class))
                .usageDescription(JsonHelper.evaluateSpelExpression(
                        actionNode, "usage?.description?:'N/A'", String.class));
        }
    }

    private static final ObjectNode getActionObjectNode(ActionLoadResult actionLoadResult) {
        try {
            return yamlObjectMapper.readValue(actionLoadResult.getActionText(), ObjectNode.class);
        } catch ( Exception e ) {
            // Return null if not valid YAML document
            return null;
        }
    }
    
    private static final String getStatus(ActionLoadResult actionLoadResult) {
        try {
            // Validate action
            actionLoadResult.getAction();
        } catch ( Exception e ) {
            return "INVALID";
        }
        return "VALID";
    } 
}
