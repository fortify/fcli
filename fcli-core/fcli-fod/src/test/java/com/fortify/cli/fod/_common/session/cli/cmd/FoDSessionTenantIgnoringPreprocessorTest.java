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
package com.fortify.cli.fod._common.session.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginOptions;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.UnmatchedArgumentException;

class FoDSessionTenantIgnoringPreprocessorTest {
    @Test
    void shouldAllowClientCredentialsWithoutTenant() {
        var cmd = parse("--client-id", "id", "--client-secret", "secret");

        assertTrue(cmd.loginOptions.hasClientCredentials());
        assertFalse(cmd.loginOptions.hasUserCredentials());
    }

    @Test
    void shouldIgnoreTenantWhenClientCredentialsProvided() {
        var cmd = parse("--tenant", "acme", "--client-id", "id", "--client-secret", "secret");

        assertTrue(cmd.loginOptions.hasClientCredentials());
        assertFalse(cmd.loginOptions.hasUserCredentials());
    }

    @Test
    void shouldIgnoreCompactTenantWhenClientCredentialsProvided() {
        var cmd = parse("-tacme", "--client-id", "id", "--client-secret", "secret");

        assertTrue(cmd.loginOptions.hasClientCredentials());
        assertFalse(cmd.loginOptions.hasUserCredentials());
    }

    @Test
    void shouldIgnoreAllSupportedTenantSyntaxWhenClientCredentialsProvided() {
        assertClientCredentialsOnly("-t", "acme");
        assertClientCredentialsOnly("-t=acme");
        assertClientCredentialsOnly("-tacme");
        assertClientCredentialsOnly("--tenant", "acme");
        assertClientCredentialsOnly("--tenant=acme");
    }

    @Test
    void shouldIgnoreAllSupportedTenantSyntaxWhenClientCredentialsWithEqualsSyntax() {
        assertClientCredentialsOnlyEqualsStyle("-t", "acme");
        assertClientCredentialsOnlyEqualsStyle("-t=acme");
        assertClientCredentialsOnlyEqualsStyle("-tacme");
        assertClientCredentialsOnlyEqualsStyle("--tenant", "acme");
        assertClientCredentialsOnlyEqualsStyle("--tenant=acme");
    }

    @Test
    void shouldRejectInvalidLongTenantSyntax() {
        assertThrows(UnmatchedArgumentException.class,
                () -> parse("--tenanttenant-value", "--client-id", "id", "--client-secret", "secret"));
    }

    @Test
    void shouldFailUserCredentialsWithoutTenant() {
        var ex = assertThrows(MissingParameterException.class,
                () -> parse("--user", "bob", "--password", "pw"));

        assertTrue(ex.getMessage().toLowerCase().contains("tenant"));
    }

    @Test
    void shouldAllowUserCredentialsWithTenant() {
        var cmd = parse("--tenant", "acme", "--user", "bob", "--password", "pw");

        assertTrue(cmd.loginOptions.hasUserCredentials());
        assertDoesNotThrow(() -> cmd.loginOptions.getUserCredentials());
    }

    private static void assertClientCredentialsOnly(String... tenantArgs) {
        var args = new ArrayList<String>();
        Collections.addAll(args, tenantArgs);
        args.add("--client-id");
        args.add("id");
        args.add("--client-secret");
        args.add("secret");

        var cmd = parse(args.toArray(String[]::new));
        assertTrue(cmd.loginOptions.hasClientCredentials());
        assertFalse(cmd.loginOptions.hasUserCredentials());
        // Verify other options (e.g. --url) are not corrupted by the preprocessing.
        assertDoesNotThrow(() -> cmd.loginOptions.getUrlConfigOptions().getUrl());
    }

    private static void assertClientCredentialsOnlyEqualsStyle(String... tenantArgs) {
        var args = new ArrayList<String>();
        Collections.addAll(args, tenantArgs);
        args.add("--client-id=id");
        args.add("--client-secret=secret");

        var cmd = parse(args.toArray(String[]::new));
        assertTrue(cmd.loginOptions.hasClientCredentials());
        assertFalse(cmd.loginOptions.hasUserCredentials());
        assertDoesNotThrow(() -> cmd.loginOptions.getUrlConfigOptions().getUrl());
    }

    private static TestFoDLoginCommand parse(String... args) {
        var cmd = new TestFoDLoginCommand();
        var fullArgs = new ArrayList<String>();
        fullArgs.add("--url");
        fullArgs.add("https://example.org");
        Collections.addAll(fullArgs, args);
        new CommandLine(cmd).parseArgs(fullArgs.toArray(String[]::new));
        return cmd;
    }

    @Command(name = "test-fod-login", preprocessor = FoDSessionTenantIgnoringPreprocessor.class)
    static final class TestFoDLoginCommand {
        @Mixin private FoDSessionLoginOptions loginOptions;
    }
}