package com.fortify.cli.ftest._common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Helper for testing the fcli RPC server via stdio.
 * Spawns fcli as a subprocess, communicates via stdin/stdout using JSON-RPC.
 */
class RPCServerHelper implements Closeable {
    private static final ObjectMapper objectMapper = new ObjectMapper()
    private final Process process
    private final BufferedWriter writer
    private final BufferedReader reader
    private final BufferedReader errorReader

    private RPCServerHelper(Process process) {
        this.process = process
        this.writer = new BufferedWriter(new OutputStreamWriter(process.outputStream, "UTF-8"))
        this.reader = new BufferedReader(new InputStreamReader(process.inputStream, "UTF-8"))
        this.errorReader = new BufferedReader(new InputStreamReader(process.errorStream, "UTF-8"))
    }

    /**
     * Start an fcli RPC server subprocess with the given arguments.
     * Waits for the server to print its startup message on stderr.
     * @param serverArgs fcli arguments (e.g. "util rpc-server start --import file.yaml")
     * @return RPCServerHelper for sending/receiving JSON-RPC messages
     */
    static RPCServerHelper start(String serverArgs) {
        def javaHome = System.getProperty("java.home")
        def classpath = System.getProperty("java.class.path")
        def cmd = [
            "${javaHome}/bin/java" as String,
            "-cp", classpath,
            "com.fortify.cli.app.FortifyCLI"
        ] + serverArgs.split(" ").toList()

        def pb = new ProcessBuilder(cmd)
        pb.redirectErrorStream(false)
        def process = pb.start()
        def helper = new RPCServerHelper(process)
        helper.waitForStartup()
        return helper
    }

    private void waitForStartup() {
        def deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            if (errorReader.ready()) {
                def line = errorReader.readLine()
                if (line != null && (line.contains("running on stdio") || line.contains("server running"))) {
                    return
                }
            }
            Thread.sleep(100)
        }
        throw new RuntimeException("Server did not start within 30 seconds")
    }

    /**
     * Send a JSON-RPC request and read the response.
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
        return sendAndReceive(objectMapper.writeValueAsString(request))
    }

    private JsonNode sendAndReceive(String jsonLine) {
        writer.write(jsonLine)
        writer.newLine()
        writer.flush()
        def responseLine = readLineWithTimeout(10_000)
        if (responseLine == null) {
            throw new RuntimeException("No response received within timeout")
        }
        return objectMapper.readTree(responseLine)
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

    @Override
    void close() {
        try { writer.close() } catch (Exception ignored) {}
        try { reader.close() } catch (Exception ignored) {}
        try { errorReader.close() } catch (Exception ignored) {}
        process.destroyForcibly()
        process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
    }
}
