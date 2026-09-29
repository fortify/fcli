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
package com.fortify.cli.ssc.action.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRestCallResponseType;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.ssc.action.helper.SSCActionProductContextProvider.SSCActionRequestHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestInstance;

class SSCActionRequestHelperRoutingTest {
    private static final String BULK_RESPONSE = """
            {"data":[
              {"responses":[{"body":{"data":{"id":1}}}]},
              {"responses":[{"body":{"data":{"id":2}}}]}
            ]}""";

    @TempDir Path tempDir;
    private HttpServer server;
    private UnirestInstance unirest;
    private SSCActionRequestHelper helper;
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        unirest = UnirestHelper.createUnirestInstance();
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        unirest.config().defaultBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());
        helper = new SSCActionRequestHelper(() -> unirest, null);
    }

    @AfterEach
    void tearDown() {
        helper.close();
        server.stop(0);
    }

    @Test
    void fileRequestsAreExecutedIndividually() throws Exception {
        var file1 = tempDir.resolve("1.bin");
        var file2 = tempDir.resolve("2.bin");
        helper.executeSimpleRequests(List.of(
                descriptor("/download/1", ActionStepRestCallResponseType.file, file1),
                descriptor("/download/2", ActionStepRestCallResponseType.file, file2)));

        assertEquals(List.of("/download/1", "/download/2"), requestedPaths);
        assertEquals("content of /download/1", Files.readString(file1));
        assertEquals("content of /download/2", Files.readString(file2));
    }

    @Test
    void multipleJsonRequestsAreStillBulked() {
        var responses = new CopyOnWriteArrayList<String>();
        helper.executeSimpleRequests(List.of(
                new ActionRequestDescriptor("GET", "/api/v1/a", null, null, ActionStepRestCallResponseType.json, null, r->responses.add(r.toString()), e->{throw e;}),
                new ActionRequestDescriptor("GET", "/api/v1/b", null, null, ActionStepRestCallResponseType.json, null, r->responses.add(r.toString()), e->{throw e;})));

        assertEquals(List.of("/api/v1/bulk"), requestedPaths);
        assertEquals(2, responses.size());
    }

    @Test
    void fileRequestIsNotIncludedInBulk() throws Exception {
        var file = tempDir.resolve("1.bin");
        var responses = new CopyOnWriteArrayList<String>();
        helper.executeSimpleRequests(List.of(
                descriptor("/download/1", ActionStepRestCallResponseType.file, file),
                new ActionRequestDescriptor("GET", "/api/v1/a", null, null, ActionStepRestCallResponseType.json, null, r->responses.add(r.toString()), e->{throw e;})));

        assertEquals(List.of("/download/1", "/api/v1/a"), requestedPaths);
        assertTrue(Files.exists(file));
        assertEquals(1, responses.size());
    }

    private ActionRequestDescriptor descriptor(String uri, ActionStepRestCallResponseType responseType, Path file) {
        return new ActionRequestDescriptor("GET", uri, null, null, responseType, file, r->{}, e->{throw e;});
    }

    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        requestedPaths.add(path);
        byte[] body;
        String contentType;
        if ( path.equals("/api/v1/bulk") ) {
            body = BULK_RESPONSE.getBytes(StandardCharsets.UTF_8);
            contentType = "application/json";
        } else if ( path.startsWith("/api/v1/") ) {
            body = "{\"data\":{\"id\":1}}".getBytes(StandardCharsets.UTF_8);
            contentType = "application/json";
        } else {
            body = ("content of "+path).getBytes(StandardCharsets.UTF_8);
            contentType = "application/octet-stream";
        }
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try ( OutputStream os = exchange.getResponseBody() ) {
            os.write(body);
        }
    }
}
