package com.fortify.cli.aviator.entitlement.cli.cmd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class AviatorEntitlementListCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorEntitlementListCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            List<Entitlement> entitlements = fetchEntitlements(client, sessionDescriptor, messageAndSignature);
            ArrayNode entitlementsArray = formatEntitlements(entitlements);
            logEntitlementCount(entitlements.size(), sessionDescriptor.getTenant());
            return entitlementsArray;
        } catch (StatusRuntimeException e) {
            String errorMessage = e.getStatus().getDescription() != null ? e.getStatus().getDescription() : "Unknown error occurred while listing entitlements";
            throw new FcliSimpleException(errorMessage);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred while listing entitlements";
            throw new FcliSimpleException(errorMessage);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(sessionDescriptor, sessionDescriptor.getTenant());
    }

    private List<Entitlement> fetchEntitlements(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.listEntitlements(sessionDescriptor.getTenant(), signature, message);
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