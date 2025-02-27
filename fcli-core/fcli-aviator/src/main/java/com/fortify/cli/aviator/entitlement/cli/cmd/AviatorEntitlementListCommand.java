package com.fortify.cli.aviator.entitlement.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.List;

@Command(name = "list")
public class AviatorEntitlementListCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorEntitlementListCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(
                    sessionDescriptorSupplier,
                    sessionDescriptor.getTenant()
            );
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];

            List<Entitlement> entitlements = client.listEntitlements(sessionDescriptor.getTenant(), signature, message);

            ArrayNode entitlementsArray = AviatorGrpcUtils.createArrayNode();
            for (Entitlement entitlement : entitlements) {
                JsonNode node = AviatorGrpcUtils.grpcToJsonNode(entitlement);
                ObjectNode formattedNode = node.deepCopy();
                JsonNode tenantNode = node.get("tenant");
                if (tenantNode != null && tenantNode.has("name")) {
                    formattedNode.put("tenant_name", tenantNode.get("name").asText());
                    formattedNode.remove("tenant");
                }
                entitlementsArray.add(formattedNode);
            }

            if (entitlements.isEmpty()) {
                LOG.info("No entitlements found for tenant: {}", sessionDescriptor.getTenant());
            } else {
                LOG.info("Successfully listed {} entitlements for tenant: {}", entitlements.size(), sessionDescriptor.getTenant());
            }

            return entitlementsArray;
        } catch (Exception e) {
            LOG.error("Error listing entitlements: {}", e.getMessage(), e);
            throw new FcliSimpleException("Failed to list entitlements: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}