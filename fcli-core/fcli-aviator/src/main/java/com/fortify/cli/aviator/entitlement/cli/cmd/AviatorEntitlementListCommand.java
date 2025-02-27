package com.fortify.cli.aviator.entitlement.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
            String message = String.format("%s;%s",
                    sessionDescriptor.getTenant(),
                    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);

            List<Entitlement> entitlements = client.listEntitlements(sessionDescriptor.getTenant(), signature, message);

            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode entitlementsArray = objectMapper.createArrayNode();

            for (Entitlement entitlement : entitlements) {
                ObjectNode entitlementNode = objectMapper.createObjectNode();
                entitlementNode.put("id", entitlement.getId());
                entitlementNode.put("startDate", entitlement.getStartDate());
                entitlementNode.put("endDate", entitlement.getEndDate());
                entitlementNode.put("numberOfProjects", entitlement.getNumberOfProjects());
                entitlementNode.put("numberOfNcds", entitlement.getNumberOfNcds());
                entitlementNode.put("contractId", entitlement.getContractId());
                entitlementNode.put("currentlyLinkedProjects", entitlement.getCurrentlyLinkedProjects());
                entitlementNode.put("isValid", entitlement.getIsValid());
                entitlementNode.put("tenantName", entitlement.getTenant().getName());
                entitlementsArray.add(entitlementNode);
            }

            if (entitlements.isEmpty()) {
                LOG.info("No entitlements found for tenant: {}", sessionDescriptor.getTenant());
            } else {
                LOG.info("Successfully listed {} entitlements for tenant: {}", entitlements.size(), sessionDescriptor.getTenant());
            }

            return entitlementsArray;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list entitlements", e);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}