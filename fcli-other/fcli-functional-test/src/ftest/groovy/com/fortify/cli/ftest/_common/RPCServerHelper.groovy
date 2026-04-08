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
        serverResource.close()
    }

    private static List<String> toArgsList(String argsString) {
        argsString.split(" ").toList()
    }
}
