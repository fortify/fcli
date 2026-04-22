package com.fortify.cli.ftest._common

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Helper for testing the fcli RPC server via stdio.
 * In build/reflective mode, starts the server in a separate thread with piped streams.
 * In external mode (jar/native), spawns fcli as a subprocess.
 * Communicates via stdin/stdout using JSON-RPC in both modes.
 */
class RPCServerHelper implements Closeable {
    private static final ObjectMapper objectMapper = new ObjectMapper()
    private final BufferedWriter writer
    private final BufferedReader reader
    private final BufferedReader errorReader
    private final Closeable serverResource

    private RPCServerHelper(BufferedWriter writer, BufferedReader reader, BufferedReader errorReader, Closeable serverResource) {
        this.writer = writer
        this.reader = reader
        this.errorReader = errorReader
        this.serverResource = serverResource
    }

    /**
     * Start an fcli RPC server with the given arguments.
     * Uses reflective in-process invocation (build mode) or subprocess (jar/native),
     * matching the approach used by Fcli.groovy for regular command execution.
     * Waits for the server to print its startup message on stderr.
     * @param serverArgs fcli arguments (e.g. "util rpc-server start --import file.yaml")
     * @return RPCServerHelper for sending/receiving JSON-RPC messages
     */
    static RPCServerHelper start(String serverArgs) {
        if ( Fcli.isReflective() ) {
            return startReflective(serverArgs)
        } else {
            return startExternal(serverArgs)
        }
    }

    private static RPCServerHelper startReflective(String serverArgs) {
        // Create piped streams for stdin/stdout/stderr communication
        def toServerPipe = new PipedOutputStream()
        def serverIn = new PipedInputStream(toServerPipe)
        def fromServerPipe = new PipedOutputStream()
        def fromServer = new PipedInputStream(fromServerPipe)
        def fromServerErrPipe = new PipedOutputStream()
        def fromServerErr = new PipedInputStream(fromServerErrPipe)

        // Configure RPCServerStartCommand stream overrides reflectively
        def cmdClass = Class.forName("com.fortify.cli.util.rpc_server.cli.cmd.RPCServerStartCommand")
        def configureMethod = cmdClass.getMethod("configureStreams", InputStream.class, OutputStream.class, OutputStream.class)
        def clearMethod = cmdClass.getMethod("clearStreamOverrides")
        configureMethod.invoke(null, serverIn, fromServerPipe, fromServerErrPipe)

        def args = toArgsList(serverArgs) as String[]
        def runnerClass = Class.forName("com.fortify.cli.app.runner.DefaultFortifyCLIRunner")
        def method = runnerClass.getMethod("run", String[].class)
        def serverError = new java.util.concurrent.atomic.AtomicReference<Throwable>()
        def serverThread = new Thread({
            try {
                method.invoke(null, [args] as Object[])
            } catch (Exception e) {
                serverError.set(e)
            } finally {
                clearMethod.invoke(null)
            }
        }, "rpc-server")
        serverThread.daemon = true
        serverThread.start()

        def writer = new BufferedWriter(new OutputStreamWriter(toServerPipe, "UTF-8"))
        def rdr = new BufferedReader(new InputStreamReader(fromServer, "UTF-8"))
        def errRdr = new BufferedReader(new InputStreamReader(fromServerErr, "UTF-8"))
        Closeable resource = {
            try { toServerPipe.close() } catch (Exception ignored) {}
            serverThread.join(5000)
            if ( serverThread.alive ) { serverThread.interrupt() }
            clearMethod.invoke(null)
        }
        def helper = new RPCServerHelper(writer, rdr, errRdr, resource)
        helper.waitForStartup(serverThread, serverError)
        return helper
    }

    private static RPCServerHelper startExternal(String serverArgs) {
        def cmd = Fcli.buildExternalCommand(toArgsList(serverArgs))
        def pb = new ProcessBuilder(cmd)
        pb.redirectErrorStream(false)
        def process = pb.start()
        def writer = new BufferedWriter(new OutputStreamWriter(process.outputStream, "UTF-8"))
        def rdr = new BufferedReader(new InputStreamReader(process.inputStream, "UTF-8"))
        def errRdr = new BufferedReader(new InputStreamReader(process.errorStream, "UTF-8"))
        Closeable resource = {
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
        }
        def helper = new RPCServerHelper(writer, rdr, errRdr, resource)
        helper.waitForStartup()
        return helper
    }

    private void waitForStartup(Thread serverThread = null, java.util.concurrent.atomic.AtomicReference<Throwable> serverError = null) {
        def deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            if (serverThread != null && !serverThread.alive) {
                def err = serverError?.get()
                throw new RuntimeException("RPC server thread died before startup completed" + 
                    (err ? ": " + err.message : ""), err)
            }
            if (errorReader.ready()) {
                def line = errorReader.readLine()
                if (line != null && line.contains("running on stdio")) {
                    return
                }
            }
            Thread.sleep(100)
        }
        throw new RuntimeException("RPC server did not start within 30 seconds" +
            (serverThread != null ? " (thread alive: " + serverThread.alive + ")" : ""))
    }

    /**
     * Send a JSON-RPC request and read the response, skipping any notifications.
     * @param method RPC method name
     * @param params Parameter map (can be null)
     * @param id Request ID
     * @return Parsed JSON response node
     */
    JsonNode rpcCall(String method, Map<String, Object> params, int id) {
        def request = objectMapper.createObjectNode()
        request.put("jsonrpc", "2.0")
        request.put("method", method)
        request.put("id", id)
        if (params != null) {
            request.set("params", objectMapper.valueToTree(params))
        }
        return sendAndReceive(objectMapper.writeValueAsString(request), id)
    }

    /**
     * Send a JSON-RPC request and collect both the response and any notifications
     * received before/after the response. Useful for testing push notifications.
     * @param method RPC method name
     * @param params Parameter map (can be null)
     * @param id Request ID
     * @param postResponseWaitMs How long to wait for additional notifications after the response
     * @return Map with 'response' (JsonNode) and 'notifications' (List<JsonNode>)
     */
    Map<String, Object> rpcCallWithNotifications(String method, Map<String, Object> params, int id, long postResponseWaitMs = 500) {
        def request = objectMapper.createObjectNode()
        request.put("jsonrpc", "2.0")
        request.put("method", method)
        request.put("id", id)
        if (params != null) {
            request.set("params", objectMapper.valueToTree(params))
        }
        writer.write(objectMapper.writeValueAsString(request))
        writer.newLine()
        writer.flush()

        def notifications = []
        JsonNode response = null
        def deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            def line = readLineWithTimeout(deadline - System.currentTimeMillis())
            if (line == null) break
            def node = objectMapper.readTree(line)
            if (!node.has("id") || node.get("id").isNull()) {
                notifications.add(node)
            } else {
                response = node
                break
            }
        }
        // After getting the response, continue reading notifications for a short while
        if (response != null) {
            def notifDeadline = System.currentTimeMillis() + postResponseWaitMs
            while (System.currentTimeMillis() < notifDeadline) {
                def line = readLineWithTimeout(notifDeadline - System.currentTimeMillis())
                if (line == null) break
                def node = objectMapper.readTree(line)
                if (!node.has("id") || node.get("id").isNull()) {
                    notifications.add(node)
                }
            }
        }
        return [response: response, notifications: notifications]
    }

    /**
     * Drain any pending notifications from the stream (non-blocking).
     * Call this between rpcCall invocations to clear notifications from the previous job.
     * @param waitMs How long to wait for notifications
     * @return List of notification JsonNodes received
     */
    List<JsonNode> drainNotifications(long waitMs = 500) {
        def notifications = []
        def deadline = System.currentTimeMillis() + waitMs
        while (System.currentTimeMillis() < deadline) {
            def line = readLineWithTimeout(deadline - System.currentTimeMillis())
            if (line == null) break
            def node = objectMapper.readTree(line)
            if (!node.has("id") || node.get("id").isNull()) {
                notifications.add(node)
            }
        }
        return notifications
    }

    private JsonNode sendAndReceive(String jsonLine, int expectedId) {
        writer.write(jsonLine)
        writer.newLine()
        writer.flush()
        // Read lines until we find the response with matching id (skip notifications)
        def deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            def responseLine = readLineWithTimeout(deadline - System.currentTimeMillis())
            if (responseLine == null) {
                throw new RuntimeException("No response received within timeout")
            }
            def node = objectMapper.readTree(responseLine)
            // Skip notifications (no id field or null id)
            if (!node.has("id") || node.get("id").isNull()) {
                continue
            }
            return node
        }
        throw new RuntimeException("No response received within timeout")
    }

    private String readLineWithTimeout(long timeoutMs) {
        def deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (reader.ready()) {
                return reader.readLine()
            }
            Thread.sleep(50)
        }
        return null
    }

    /**
     * Start an async job (fcli.execute or fn.call), wait for completion via job.getPage polling,
     * and return the final job.getPage result.
     * @param method RPC method name (e.g. "fcli.execute" or "fn.call")
     * @param params Parameter map
     * @param startId Request ID for the start call
     * @param pageId Request ID for the getPage call
     * @param timeoutMs Maximum time to wait for completion
     * @return The final job.getPage result node (with status, records, stdout, etc.)
     */
    JsonNode executeAndWait(String method, Map<String, Object> params, int startId, int pageId, long timeoutMs = 10_000) {
        // Ensure caching is enabled so job.getPage can retrieve results
        def effectiveParams = new LinkedHashMap<String, Object>(params ?: [:])
        if (!effectiveParams.containsKey("cache")) {
            effectiveParams.put("cache", [ttl: "10m"])
        }
        def startResponse = rpcCall(method, effectiveParams, startId)
        assert startResponse.get("error") == null : "Unexpected error starting job: ${startResponse}"
        def jobId = startResponse.get("result").get("jobId").asText()
        assert jobId != null && !jobId.isEmpty()

        def deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            def pageResponse = rpcCall("job.getPage", [jobId: jobId, offset: 0, limit: 10000], pageId)
            assert pageResponse.get("error") == null : "Unexpected error polling job: ${pageResponse}"
            def result = pageResponse.get("result")
            if (result.get("pagination")?.get("complete")?.asBoolean()) {
                return result
            }
            Thread.sleep(100)
        }
        throw new RuntimeException("Job ${jobId} did not complete within ${timeoutMs}ms")
    }

    @Override
    void close() {
        try { writer.close() } catch (Exception ignored) {}
        try { reader.close() } catch (Exception ignored) {}
        try { errorReader.close() } catch (Exception ignored) {}
        serverResource.close()
    }

    private static List<String> toArgsList(String argsString) {
        argsString.split(" ").toList()
    }
}
