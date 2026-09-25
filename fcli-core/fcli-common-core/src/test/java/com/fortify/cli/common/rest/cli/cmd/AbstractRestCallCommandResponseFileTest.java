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
package com.fortify.cli.common.rest.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ListResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.product.NoOpProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParameterException;

class AbstractRestCallCommandResponseFileTest {
    private static final byte[] ZIP_BYTES = {'P', 'K', 3, 4, 20, 0, 0, 0, 8, 0};
    private static final String JSON_BODY = "{\"items\":[{\"id\":1}]}";

    @TempDir Path tempDir;
    private HttpServer server;
    private UnirestInstance unirest;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        // Default test logging writes HTTP client debug output to stdout, which would mix with command output
        ((Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fpr", ex -> respond(ex, "application/octet-stream", ZIP_BYTES));
        server.createContext("/json", ex -> {
            // Paging header must be ignored in response file mode
            ex.getResponseHeaders().add("Link", "<"+url("/json?page=2")+">; rel=\"next\"");
            respond(ex, "application/json", JSON_BODY.getBytes(StandardCharsets.UTF_8));
        });
        server.start();
        unirest = UnirestHelper.createUnirestInstance();
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        unirest.config().defaultBaseUrl(url(""));
    }

    @AfterEach
    void tearDown() {
        unirest.close();
        server.stop(0);
    }

    @Test
    void responseFileSavesBodyAndOutputsRecord() throws Exception {
        var dest = tempDir.resolve("out.bin");
        var output = run("/fpr", "--response-file="+dest, "-o", "json");

        assertArrayEquals(ZIP_BYTES, Files.readAllBytes(dest));
        var record = JsonHelper.getObjectMapper().readTree(output);
        if ( record.isArray() ) { record = record.get(0); }
        assertEquals(dest.toAbsolutePath().normalize().toString(), record.get("file").asText());
        assertEquals(ZIP_BYTES.length, record.get("size").asLong());
        assertEquals("application/octet-stream", record.get("contentType").asText());
        assertEquals(200, record.get("status").asInt());
    }

    @Test
    void responseFileSendsSingleRequestDespitePagingHeaders() throws Exception {
        var dest = tempDir.resolve("page.json");
        run("/json", "--response-file="+dest, "-o", "json");

        assertEquals(1, requestCount.get());
        assertEquals(JSON_BODY, Files.readString(dest));
    }

    @Test
    void responseFileRejectsTransform() {
        var dest = tempDir.resolve("x");
        var e = assertThrows(ParameterException.class, () -> run("/fpr", "--response-file="+dest, "--transform=items"));
        assertTrue(e.getMessage().contains("--transform") && e.getMessage().contains("--response-file"), e.getMessage());
        assertEquals(0, requestCount.get());
        assertFalse(Files.exists(dest));
    }

    @Test
    void responseFileRejectsNoTransform() {
        var dest = tempDir.resolve("x");
        var e = assertThrows(ParameterException.class, () -> run("/fpr", "--response-file="+dest, "--no-transform"));
        assertTrue(e.getMessage().contains("--no-transform"), e.getMessage());
        assertEquals(0, requestCount.get());
    }

    @Test
    void jsonModeIsUnchangedWithoutResponseFile() throws Exception {
        var output = run("/json", "--no-paging", "-o", "json");
        JsonNode result = JsonHelper.getObjectMapper().readTree(output);
        assertEquals(JsonHelper.getObjectMapper().readTree(JSON_BODY), result.isArray() && result.size()==1 ? result.get(0) : result);
        assertEquals(1, requestCount.get());
    }

    private String run(String uri, String... options) throws Exception {
        var cmd = new TestRestCallCommand(unirest);
        var args = new String[options.length+1];
        args[0] = uri;
        System.arraycopy(options, 0, args, 1, options.length);
        var commandLine = new CommandLine(cmd);
        // Output writers look up (optional) messages like default table columns
        commandLine.setResourceBundle(new ListResourceBundle() {
            @Override protected Object[][] getContents() { return new Object[0][]; }
        });
        commandLine.parseArgs(args);
        // Mimic FcliExecutionStrategy, which injects the CommandSpec into ICommandAware user objects
        var spec = commandLine.getCommandSpec();
        FcliCommandSpecHelper.getAllUserObjectsStream(spec)
            .filter(ICommandAware.class::isInstance)
            .forEach(o -> ((ICommandAware)o).setCommandSpec(spec));
        var originalOut = System.out;
        var captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            cmd.call();
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private void respond(HttpExchange exchange, String contentType, byte[] body) throws IOException {
        requestCount.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try ( OutputStream os = exchange.getResponseBody() ) {
            os.write(body);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:"+server.getAddress().getPort()+path;
    }

    @Command(name = OutputHelperMixins.RestCall.CMD_NAME)
    static final class TestRestCallCommand extends AbstractRestCallCommand {
        @Getter @Mixin private OutputHelperMixins.RestCall outputHelper;
        private final UnirestInstance unirest;

        TestRestCallCommand(UnirestInstance unirest) {
            this.unirest = unirest;
        }

        @Override
        protected IUnirestInstanceSupplier getUnirestInstanceSupplier() {
            return () -> unirest;
        }

        @Override
        protected IProductHelper getProductHelper() {
            return NoOpProductHelper.instance();
        }
    }
}
