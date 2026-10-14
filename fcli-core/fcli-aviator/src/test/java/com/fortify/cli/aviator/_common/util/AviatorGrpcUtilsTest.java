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
package com.fortify.cli.aviator._common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.Application;
import com.fortify.aviator.application.ApplicationMultiplierOperationResponse;
import com.fortify.aviator.application.ApplicationMultiplierOutcome;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._main.cli.cmd.AviatorCommands;

import picocli.CommandLine;

@DisplayName("AviatorGrpcUtils")
class AviatorGrpcUtilsTest {
    @Test
    void incrementTableRetainsLegacyColumns() {
        var messages = ResourceBundle.getBundle("com.fortify.cli.aviator.i18n.AviatorMessages");

        assertEquals("id,name,entitlement_id,entitlement_multiplier,entitlements_consumed,quota_last_updated,quota",
                messages.getString("fcli.aviator.app.add-entitlement.output.table.args"));
    }

    @Test
    void reductionCommandIsRegisteredAndRequiresApplicationId() {
        CommandLine root = new CommandLine(CommandLine.Model.CommandSpec.create().name("fcli"))
            .addSubcommand(new AviatorCommands());
        CommandLine command = root.getSubcommands().get("aviator").getSubcommands().get("app")
            .getSubcommands().get("reduce-entitlement");
        assertTrue(command != null, "reduce-entitlement must be registered");
        assertThrows(CommandLine.MissingParameterException.class, () -> root.parseArgs("aviator", "app", "reduce-entitlement"));
        root.parseArgs("aviator", "app", "reduce-entitlement", "101", "--output", "json");
    }

    @Test
    void reductionStructuredOutputContainsStableOutcomeAndMultiplierFields() {
        var response = ApplicationMultiplierOperationResponse.newBuilder().setApplicationId(101L)
                .setPreviousMultiplier(2).setNewMultiplier(1).setQuota(500)
                .setOutcome(ApplicationMultiplierOutcome.MULTIPLIER_REDUCED)
                .setMessage("One entitlement allocation unit was removed").build();

        var json = AviatorGrpcUtils.grpcToJsonNode(response);

        assertEquals(101L, json.path("application_id").asLong());
        assertEquals(2, json.path("previous_multiplier").asInt());
        assertEquals(1, json.path("new_multiplier").asInt());
        assertEquals(500, json.path("quota").asInt());
        assertEquals("MULTIPLIER_REDUCED", json.path("outcome").asText());
        assertTrue(json.path("message").asText().contains("unit was removed"));
    }

    @Test
    void reactivationStructuredOutputIncludesPreservedQuotaWithoutMessageParsing() {
        var response = Application.newBuilder().setId(101L).setEntitlementMultiplier(2).setQuota(600)
                .setPreviousMultiplier(1).setMultiplierOutcome(ApplicationMultiplierOutcome.INACTIVE_ALLOCATION_REACTIVATED)
                .setOperationMessage("An inactive allocation was reactivated with preserved quota 100; quota was not reset")
                .setOperationAllocationQuota(100).addActiveEntitlementIds(501).addActiveEntitlementIds(502).build();

        var json = AviatorGrpcUtils.grpcToJsonNode(response);

        assertEquals("INACTIVE_ALLOCATION_REACTIVATED", json.path("multiplier_outcome").asText());
        assertEquals(100, json.path("operation_allocation_quota").asInt());
        assertTrue(json.path("operation_message").asText().contains("quota was not reset"));
        assertEquals(2, json.path("active_entitlement_ids").size());
    }

    @Test
    @DisplayName("wraps conversion failures in AviatorTechnicalException")
    void wrapsConversionFailuresInTechnicalException() {
        AviatorTechnicalException exception = assertThrows(AviatorTechnicalException.class, () -> AviatorGrpcUtils.grpcToJsonNode(null));

        assertTrue(exception.getMessage().contains("Failed to convert gRPC message to JSON"));
        assertTrue(exception.getCause() instanceof NullPointerException);
    }
}