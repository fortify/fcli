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
package com.fortify.cli.fod._common.session.helper.sso;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;

/**
 * A Chrome/Edge instance started with a loopback-only DevTools port, used to let the user complete
 * the FoD SSO login and to read the SAML assertion that FoD displays on its assertion consumer
 * service page. The assertion is never logged.
 */
final class FoDSsoBrowser implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FoDSsoBrowser.class);
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();
    private static final int DEVTOOLS_TIMEOUT_MS = 5000;
    private static final long STARTUP_TIMEOUT_MS = 30000;
    private static final long POLL_INTERVAL_MS = 500;
    // Text of the assertion element (or page body) once the page has fully loaded
    private static final String READ_PAGE_EXPRESSION =
            "({state: document.readyState, text: ((document.querySelector('pre') || document.body || {}).innerText) || ''})";

    private final Process process;
    private final Path profileDir;
    private final boolean deleteProfile;
    private final int port;
    private final String browserWsPath;

    private FoDSsoBrowser(Process process, Path profileDir, boolean deleteProfile, int port, String browserWsPath) {
        this.process = process;
        this.profileDir = profileDir;
        this.deleteProfile = deleteProfile;
        this.port = port;
        this.browserWsPath = browserWsPath;
    }

    /**
     * @param profileDir Persistent browser profile directory, or null to use a temporary profile that is deleted on close
     */
    static FoDSsoBrowser launch(Path browser, Path profileDir, URI url) {
        var temporary = profileDir == null;
        Path dir;
        try {
            dir = temporary ? Files.createTempDirectory("fcli-fod-sso-") : Files.createDirectories(profileDir.toAbsolutePath());
        } catch ( IOException e ) {
            throw new FcliSimpleException("Error creating browser profile directory", e);
        }
        Process process = null;
        try {
            var activePortFile = dir.resolve("DevToolsActivePort");
            // A stale file from a previous run would make us connect to a non-existent port
            Files.deleteIfExists(activePortFile);
            process = new ProcessBuilder(browser.toString(), "--remote-debugging-port=0", "--user-data-dir=" + dir,
                    "--no-first-run", "--no-default-browser-check", "--new-window", url.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            var lines = awaitActivePort(process, activePortFile);
            return new FoDSsoBrowser(process, dir, temporary, Integer.parseInt(lines.get(0).trim()),
                    lines.size() > 1 ? lines.get(1).trim() : null);
        } catch ( Exception e ) {
            if ( process != null ) { process.destroyForcibly(); }
            if ( temporary ) { deleteRecursively(dir); }
            if ( e instanceof RuntimeException ) { throw (RuntimeException) e; }
            throw new FcliTechnicalException("Error launching browser " + browser, e);
        }
    }

    /**
     * Wait for the user to complete the login and return the text displayed on the page whose URL
     * is an assertion page for the given SAML IdP URI.
     */
    String waitForAssertion(URI samlIdpUri, Duration timeout) {
        var deadline = System.nanoTime() + timeout.toNanos();
        while ( System.nanoTime() < deadline ) {
            try {
                var text = readAssertionPageText(samlIdpUri);
                if ( StringUtils.isNotBlank(text) ) { return text; }
            } catch ( IOException e ) {
                LOG.debug("DevTools request failed: {}", e.toString());
                if ( !process.isAlive() ) {
                    throw new FcliSimpleException("The browser was closed before SSO login completed");
                }
            }
            sleep(POLL_INTERVAL_MS);
        }
        throw new FcliSimpleException("Timed out waiting for SSO login to complete in the browser");
    }

    @Override
    public void close() {
        if ( browserWsPath != null && process.isAlive() ) {
            // Graceful close, so that a persistent profile is properly flushed
            try ( var client = FoDSsoDevToolsClient.connect(URI.create("ws://127.0.0.1:" + port + browserWsPath), 2000) ) {
                client.call("Browser.close", null);
            } catch ( IOException | RuntimeException e ) {
                LOG.debug("Error closing browser gracefully: {}", e.toString());
            }
        }
        try {
            if ( !process.waitFor(5, TimeUnit.SECONDS) ) {
                process.destroy();
                if ( !process.waitFor(3, TimeUnit.SECONDS) ) { process.destroyForcibly(); }
            }
        } catch ( InterruptedException e ) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        if ( deleteProfile ) { deleteRecursively(profileDir); }
    }

    private String readAssertionPageText(URI samlIdpUri) throws IOException {
        for ( var page : listPages() ) {
            var wsUrl = page.path("webSocketDebuggerUrl").asText("");
            if ( "page".equals(page.path("type").asText()) && !wsUrl.isEmpty()
                    && FoDSsoLoginHelper.isAssertionPage(samlIdpUri, page.path("url").asText()) ) {
                try ( var client = FoDSsoDevToolsClient.connect(URI.create(wsUrl), DEVTOOLS_TIMEOUT_MS) ) {
                    var params = OM.createObjectNode();
                    params.put("expression", READ_PAGE_EXPRESSION);
                    params.put("returnByValue", true);
                    var value = client.call("Runtime.evaluate", params).path("result").path("value");
                    if ( "complete".equals(value.path("state").asText()) ) { return value.path("text").asText(); }
                }
            }
        }
        return null;
    }

    private JsonNode listPages() throws IOException {
        var connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/json/list").openConnection();
        connection.setConnectTimeout(DEVTOOLS_TIMEOUT_MS);
        connection.setReadTimeout(DEVTOOLS_TIMEOUT_MS);
        try ( var in = connection.getInputStream() ) {
            return OM.readTree(in);
        } finally {
            connection.disconnect();
        }
    }

    private static List<String> awaitActivePort(Process process, Path activePortFile) throws IOException {
        var deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS;
        while ( System.currentTimeMillis() < deadline ) {
            if ( Files.exists(activePortFile) ) {
                var lines = Files.readAllLines(activePortFile);
                if ( !lines.isEmpty() && lines.get(0).trim().matches("\\d+") ) { return lines; }
            }
            if ( !process.isAlive() ) {
                throw new FcliSimpleException("The browser exited before SSO login could start. "
                        + "If you use --browser-profile, make sure no other browser instance is using that profile.");
            }
            sleep(200);
        }
        throw new FcliSimpleException("Timed out waiting for the browser to start");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new FcliSimpleException("Interrupted while waiting for SSO login to complete");
        }
    }

    // The browser may keep files locked for a short while after exiting, so retry
    private static void deleteRecursively(Path dir) {
        for ( int i = 0; i < 10; i++ ) {
            try ( var paths = Files.walk(dir) ) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch ( IOException | RuntimeException e ) {
                LOG.debug("Error deleting temporary browser profile: {}", e.toString());
            }
            if ( !Files.exists(dir) ) { return; }
            try { Thread.sleep(300); } catch ( InterruptedException e ) { Thread.currentThread().interrupt(); return; }
        }
        LOG.debug("Could not delete temporary browser profile {}", dir);
    }
}
