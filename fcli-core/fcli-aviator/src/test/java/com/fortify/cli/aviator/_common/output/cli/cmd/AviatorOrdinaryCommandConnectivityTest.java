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
package com.fortify.cli.aviator._common.output.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorConnectivityException;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator.grpc.AviatorRequestPolicy;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;

/**
 * Checks that ordinary Aviator commands fail fast with the shared connectivity messages.
 * Each case targets a closed loopback port, so no Aviator server is needed.
 */
class AviatorOrdinaryCommandConnectivityTest {
    private static final String FCLI_DATA_DIR_PROPERTY = "fcli.env.FCLI_DATA_DIR";
    private static final Duration FAIL_FAST_LIMIT = Duration.ofSeconds(5);
    private static final List<String> PROXY_ENV_VARS =
            List.of("https_proxy", "HTTPS_PROXY", "http_proxy", "HTTP_PROXY", "all_proxy", "ALL_PROXY");

    // Static, because some fcli helpers cache paths below the fcli data directory in static fields
    @TempDir static Path fcliDataDir;
    private static String previousFcliDataDir;
    private String closedPortUrl;

    @BeforeAll
    static void setUpFcliDataDir() {
        previousFcliDataDir = System.getProperty(FCLI_DATA_DIR_PROPERTY);
        System.setProperty(FCLI_DATA_DIR_PROPERTY, fcliDataDir.toString());
    }

    @AfterAll
    static void restoreFcliDataDir() {
        if ( previousFcliDataDir==null ) {
            System.clearProperty(FCLI_DATA_DIR_PROPERTY);
        } else {
            System.setProperty(FCLI_DATA_DIR_PROPERTY, previousFcliDataDir);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // The client reads proxy environment variables; a configured proxy would change the result
        assumeTrue(PROXY_ENV_VARS.stream().allMatch(name -> System.getenv(name)==null),
            "Skipped because a proxy environment variable is set");
        FcliExecutionContextHolder.pushNew();
        try ( var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()) ) {
            closedPortUrl = "127.0.0.1:" + socket.getLocalPort();
        }
    }

    @AfterEach
    void tearDown() {
        FcliExecutionContextHolder.pop();
    }

    @Test
    void adminCommandBaseClassCreatesOrdinaryClient() {
        var descriptor = new AviatorAdminConfigDescriptor(closedPortUrl, "tenant", "unused-private-key");
        var command = new TestAdminCommand();

        assertFailsFast(() -> {
            try ( var client = command.createGrpcClient(descriptor) ) {
                client.listApplication("tenant", "signature", "message");
            }
        });
    }

    @Test
    void userSessionTokenValidationFailsFastWithOrdinaryPolicy() {
        assertFailsFast(() -> AviatorUserSessionHelper.instance()
            .validateToken(closedPortUrl, syntheticToken(), AviatorRequestPolicy.ORDINARY));
    }

    private static String syntheticToken() {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
            + "." + encoder.encodeToString("{\"tenantName\":\"tenant\"}".getBytes(StandardCharsets.UTF_8))
            + ".signature";
    }

    private static void assertFailsFast(Executable executable) {
        long start = System.nanoTime();
        var exception = assertThrows(AviatorConnectivityException.class, executable);
        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertTrue(elapsed.compareTo(FAIL_FAST_LIMIT) < 0, "Call took " + elapsed);
        assertTrue(exception.getMessage().contains("was refused"), exception.getMessage());
    }

    private static final class TestAdminCommand extends AbstractAviatorAdminSessionOutputCommand {
        @Override
        protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) {
            return null;
        }

        @Override
        public boolean isSingular() {
            return true;
        }

        @Override
        public IOutputHelper getOutputHelper() {
            return null;
        }
    }
}
