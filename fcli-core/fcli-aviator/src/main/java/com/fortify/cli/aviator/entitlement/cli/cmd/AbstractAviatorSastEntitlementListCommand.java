/*
 * Copyright 2021-2026 Open Text.
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
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;

/**
 * Abstract base class for SAST entitlement list commands. Provides the shared
 * logic for fetching and formatting SAST entitlements via gRPC, allowing
 * concrete subclasses to differ only in command name and output helper mixin.
 */
public abstract class AbstractAviatorSastEntitlementListCommand extends AbstractAviatorAdminSessionOutputCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAviatorSastEntitlementListCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            var messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant());
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            List<Entitlement> entitlements = client.listEntitlements(configDescriptor.getTenant(), signature, message);
            ArrayNode result = formatEntitlements(entitlements);
            logEntitlementCount(entitlements.size(), configDescriptor.getTenant());
            return result;
        }
    }

    private ArrayNode formatEntitlements(List<Entitlement> entitlements) {
        ArrayNode array = AviatorGrpcUtils.createArrayNode();
        for (Entitlement entitlement : entitlements) {
            JsonNode node = AviatorGrpcUtils.grpcToJsonNode(entitlement);
            ObjectNode formatted = node.deepCopy();
            formatTenantNode(formatted, node);
            array.add(formatted);
        }
        return array;
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
            LOG.info("No SAST entitlements found for tenant: {}", tenant);
        } else {
            LOG.info("Successfully listed {} SAST entitlements for tenant: {}", count, tenant);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
