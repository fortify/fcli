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
package com.fortify.cli.common.action.runner.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRestCallResponseType;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

class BasicActionRequestHelperTest {
    private static final byte[] ZIP_BYTES = {'P', 'K', 3, 4, 20, 0, 0, 0, 8, 0};
    private static final byte[] OLD_CONTENT = "previous".getBytes(StandardCharsets.UTF_8);

    @TempDir Path tempDir;
    private HttpServer server;
    private UnirestInstance unirest;
    private BasicActionRequestHelper helper;
    private final AtomicReference<JsonNode> response = new AtomicReference<>();
    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fpr", ex -> respond(ex, 200, "application/octet-stream", ZIP_BYTES));
        server.createContext("/error", ex -> respond(ex, 500, "application/json", "{}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/latin1", ex -> respond(ex, 200, "text/plain; charset=ISO-8859-1", "caf\u00e9".getBytes(StandardCharsets.ISO_8859_1)));
        server.createContext("/json", ex -> respond(ex, 200, "application/json", "{\"id\": 1}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/csv", ex -> respond(ex, 200, "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/empty", ex -> respond(ex, 200, "text/plain", new byte[0]));
        server.start();
        unirest = UnirestHelper.createUnirestInstance();
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        unirest.config().defaultBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());
        helper = new BasicActionRequestHelper(() -> unirest, null);
    }

    @AfterEach
    void tearDown() {
        helper.close();
        server.stop(0);
    }

    @Test
    void fileResponseIsSavedAndReportedAsRecord() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        helper.executeSimpleRequests(List.of(descriptor("/fpr", ActionStepRestCallResponseType.file, dest)));

        assertNull(failure.get());
        assertArrayEquals(ZIP_BYTES, Files.readAllBytes(dest));
        var record = response.get();
        assertEquals(dest.toString(), record.get("file").asText());
        assertEquals(ZIP_BYTES.length, record.get("size").asLong());
        assertEquals("application/octet-stream", record.get("contentType").asText());
        assertEquals(200, record.get("status").asInt());
    }

    @Test
    void fileResponseErrorIsPassedToFailureConsumerAndKeepsDestination() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        Files.write(dest, OLD_CONTENT);
        helper.executeSimpleRequests(List.of(descriptor("/error", ActionStepRestCallResponseType.file, dest)));

        assertNull(response.get());
        assertInstanceOf(UnexpectedHttpResponseException.class, failure.get());
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
    }

    @Test
    void textResponseDecodesDeclaredCharset() {
        execute("/latin1", ActionStepRestCallResponseType.text);
        assertEquals(new TextNode("caf\u00e9"), response.get());
    }

    @Test
    void textResponseKeepsJsonAsRawText() {
        execute("/json", ActionStepRestCallResponseType.text);
        assertEquals(new TextNode("{\"id\": 1}"), response.get());
    }

    @Test
    void textResponseReturnsEmptyStringForEmptyBody() {
        execute("/empty", ActionStepRestCallResponseType.text);
        assertEquals(new TextNode(""), response.get());
    }

    @Test
    void textResponseRejectsBinaryBody() {
        execute("/fpr", ActionStepRestCallResponseType.text);
        assertNull(response.get());
        assertInstanceOf(FcliSimpleException.class, failure.get());
        assertTrue(failure.get().getMessage().contains("use response.type: file"), failure.get().getMessage());
    }

    @Test
    void autoResponseParsesJson() {
        execute("/json", ActionStepRestCallResponseType.auto);
        assertNull(failure.get());
        assertEquals(1, response.get().get("id").asInt());
    }

    @Test
    void autoResponseFallsBackToTextForTextualBody() {
        execute("/csv", ActionStepRestCallResponseType.auto);
        assertNull(failure.get());
        assertEquals(new TextNode("a,b\n1,2\n"), response.get());
    }

    @Test
    void autoResponseRejectsBinaryBody() {
        execute("/fpr", ActionStepRestCallResponseType.auto);
        assertNull(response.get());
        assertInstanceOf(FcliSimpleException.class, failure.get());
        assertTrue(failure.get().getMessage().contains("use response.type: file"), failure.get().getMessage());
    }

    private void execute(String uri, ActionStepRestCallResponseType responseType) {
        helper.executeSimpleRequests(List.of(descriptor(uri, responseType, null)));
    }

    private ActionRequestDescriptor descriptor(String uri, ActionStepRestCallResponseType responseType, Path responseFile) {
        return new ActionRequestDescriptor("GET", uri, null, null, responseType, responseFile, response::set, failure::set);
    }

    private void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try ( OutputStream os = exchange.getResponseBody() ) {
            os.write(body);
        }
    }
}
