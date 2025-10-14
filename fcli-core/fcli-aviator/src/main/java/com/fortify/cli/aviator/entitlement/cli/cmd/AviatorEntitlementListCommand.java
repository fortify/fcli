/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.entitlement.cli.cmd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class AviatorEntitlementListCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorEntitlementListCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            List<Entitlement> entitlements = fetchEntitlements(client, configDescriptor, messageAndSignature);
            ArrayNode entitlementsArray = formatEntitlements(entitlements);
            logEntitlementCount(entitlements.size(), configDescriptor.getTenant());
            return entitlementsArray;
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant());
    }

    private List<Entitlement> fetchEntitlements(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.listEntitlements(configDescriptor.getTenant(), signature, message);
    }

    private ArrayNode formatEntitlements(List<Entitlement> entitlements) {
        ArrayNode entitlementsArray = AviatorGrpcUtils.createArrayNode();
        for (Entitlement entitlement : entitlements) {
            JsonNode node = AviatorGrpcUtils.grpcToJsonNode(entitlement);
            ObjectNode formattedNode = node.deepCopy();
            formatTenantNode(formattedNode, node);
            entitlementsArray.add(formattedNode);
        }
        return entitlementsArray;
    }

    private void formatTenantNode(ObjectNode formattedNode, JsonNode node) {
        JsonNode tenantNode = node.get("tenant");
        if (tenantNode != null && tenantNode.has("name")) {
            formattedNode.put("tenant_name", tenantNode.get("name").asText());
            formattedNode.remove("tenant");
        }
    }

    private void logEntitlementCount(int count, String tenant) {
        if (count == 0) {
            LOG.info("No entitlements found for tenant: {}", tenant);
        } else {
            LOG.info("Successfully listed {} entitlements for tenant: {}", count, tenant);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}