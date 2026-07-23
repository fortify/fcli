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
package com.fortify.cli.aviator.connection.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator.diagnose.AviatorConnectionDiagnostics;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStage;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStageResult;
import com.fortify.cli.aviator.diagnose.AviatorDiagnosticStatus;
import com.fortify.cli.aviator.diagnose.AviatorGrpcFailureCategory;
import com.fortify.cli.aviator.diagnose.AviatorGrpcReachabilityResult;
import com.fortify.cli.aviator.diagnose.AviatorTunnelResult;
import com.fortify.cli.aviator.diagnose.IAviatorDiagnosticProbe;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;

class AviatorConnectionDiagnoseHelperTest {

    @Test
    void bareUrlOmitsCredentialStage() {
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.ok("OK", "ok"));
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromUrl("https://aviator.example.com"), 5);

        assertTrue(result.stages().stream().noneMatch(s ->
            s.stage() == AviatorDiagnosticStage.TOKEN || s.stage() == AviatorDiagnosticStage.ADMIN));
        assertFalse(result.requiredFailure());
    }

    @Test
    void urlAndTokenSkipsCredentialWhenGrpcDidNotRespond() {
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.noResponse(
            "DEADLINE_EXCEEDED", AviatorGrpcFailureCategory.NO_RESPONSE, "deadline"));
        var result = helper.diagnose(
            AviatorConnectionDiagnoseSource.fromUrlAndToken("https://aviator.example.com", "tok"), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.TOKEN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.WARN, cred.status());
        assertFalse(cred.required());
        assertTrue(cred.summary().toLowerCase().contains("skipped"));
    }

    @Test
    void adminConfigSkipsAsAdminStageWhenGrpcDidNotRespond() {
        var admin = AviatorAdminConfigDescriptor.builder()
            .aviatorUrl("https://aviator.example.com")
            .tenant("demo")
            .build();
        var helper = helperWithGrpc(AviatorGrpcReachabilityResult.noResponse(
            "DEADLINE_EXCEEDED", AviatorGrpcFailureCategory.NO_RESPONSE, "deadline"));
        var result = helper.diagnose(AviatorConnectionDiagnoseSource.fromAdminConfig(admin), 5);

        var cred = lastStage(result.stages());
        assertEquals(AviatorDiagnosticStage.ADMIN, cred.stage());
        assertEquals(AviatorDiagnosticStatus.WARN, cred.status());
        assertFalse(result.stages().stream().anyMatch(s -> s.stage() == AviatorDiagnosticStage.TOKEN));
    }

    private static AviatorDiagnosticStageResult lastStage(List<AviatorDiagnosticStageResult> stages) {
        return stages.get(stages.size() - 1);
    }

    private static AviatorConnectionDiagnoseHelper helperWithGrpc(AviatorGrpcReachabilityResult grpc) {
        var probe = new FakeProbe(grpc);
        return new AviatorConnectionDiagnoseHelper(new OfflineDiagnostics(probe));
    }

    /**
     * Uses a fixed connection plan so tests never touch ambient proxy env or real DNS policy.
     */
    private static final class OfflineDiagnostics extends AviatorConnectionDiagnostics {
        OfflineDiagnostics(IAviatorDiagnosticProbe probe) {
            super(probe);
        }

        @Override
        public List<AviatorDiagnosticStageResult> diagnose(String url, int timeoutSeconds, String sourceType) {
            var plan = new AviatorConnectionPlan(url, url,
                new ParsedTarget("aviator.example.com", null), 443, Optional.empty());
            return diagnose(plan, timeoutSeconds, sourceType);
        }
    }

    private static final class FakeProbe implements IAviatorDiagnosticProbe {
        private final AviatorGrpcReachabilityResult grpcResult;

        FakeProbe(AviatorGrpcReachabilityResult grpcResult) {
            this.grpcResult = grpcResult;
        }

        @Override
        public InetAddress[] resolve(String host) {
            try {
                return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void connect(String host, int port, int timeoutSeconds) {}

        @Override
        public AviatorTunnelResult probeTunnel(AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
            return new AviatorTunnelResult.TlsSucceeded(false, "not-used",
                "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.example.com", "h2");
        }

        @Override
        public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) {
            return grpcResult;
        }
    }
}
