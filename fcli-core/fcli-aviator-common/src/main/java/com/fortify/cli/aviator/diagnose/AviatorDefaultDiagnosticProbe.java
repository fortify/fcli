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
package com.fortify.cli.aviator.diagnose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class AviatorDefaultDiagnosticProbe implements AviatorDiagnosticProbe {
    private static final Pattern NON_GRPC_HTTP_RESPONSE_PATTERN = Pattern.compile(
        "HTTP status code (\\d+) invalid content-type: ([^\\s;]+(?:;\\s*[^\\s]+)?)", Pattern.CASE_INSENSITIVE);

    @Override
    public InetAddress[] resolve(String host) throws IOException {
        return InetAddress.getAllByName(host);
    }

    @Override
    public void connect(String host, int port, int timeoutSeconds) throws IOException {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), toMillis(timeoutSeconds));
        }
    }

    @Override
    public AviatorTlsHandshakeResult handshake(AviatorConnectionPlan connectionPlan, int timeoutSeconds) throws IOException {
        var proxyDescriptor = connectionPlan.proxyDescriptor();
        Socket rawSocket = new Socket();
        var proxyConnectStatus = "not-used";
        if (proxyDescriptor.isPresent()) {
            var proxy = proxyDescriptor.get();
            rawSocket.connect(new InetSocketAddress(proxy.getProxyHost(), proxy.getProxyPort()), toMillis(timeoutSeconds));
            rawSocket.setSoTimeout(toMillis(timeoutSeconds));
            proxyConnectStatus = connectProxyTunnel(rawSocket, connectionPlan, timeoutSeconds);
        } else {
            rawSocket.connect(new InetSocketAddress(connectionPlan.target().host(), connectionPlan.effectivePort()), toMillis(timeoutSeconds));
            rawSocket.setSoTimeout(toMillis(timeoutSeconds));
        }

        var sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (rawSocket;
            var sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                rawSocket, connectionPlan.target().host(), connectionPlan.effectivePort(), true)) {
            sslSocket.setSoTimeout(toMillis(timeoutSeconds));
            SSLParameters parameters = sslSocket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            parameters.setServerNames(List.of(new SNIHostName(connectionPlan.target().host())));
            parameters.setApplicationProtocols(new String[] {"h2"});
            sslSocket.setSSLParameters(parameters);
            sslSocket.startHandshake();

            var session = sslSocket.getSession();
            var certificates = session.getPeerCertificates();
            var peerSubject = certificates.length > 0 && certificates[0] instanceof X509Certificate certificate
                    ? certificate.getSubjectX500Principal().getName()
                    : "unknown";
            return new AviatorTlsHandshakeResult(
                session.getProtocol(), session.getCipherSuite(), peerSubject, sslSocket.getApplicationProtocol(), proxyConnectStatus);
        }
    }

    @Override
    public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) throws Exception {
        try (var client = AviatorGrpcClientHelper.createClient(url)) {
            client.probeGetDefaultQuota(timeoutSeconds);
            return new AviatorGrpcReachabilityResult(true, "OK", "Default quota response received");
        } catch (StatusRuntimeException e) {
            var status = e.getStatus();
            var responseReceived = isResponseReceived(status.getCode());
            var description = summarizeDescription(status.getDescription());
            var httpResponse = parseNonGrpcHttpResponse(description);
            if (httpResponse != null) {
                return new AviatorGrpcReachabilityResult(false, true, status.getCode().name(), "non-grpc-http-response",
                    httpResponse.statusCode(), httpResponse.contentType(), description);
            }
            var failureCategory = isTlsFailure(description) ? "grpc-tls-handshake-failed" : "grpc-no-response";
            return new AviatorGrpcReachabilityResult(responseReceived, false, status.getCode().name(), failureCategory, null, null, description);
        }
    }

    private static NonGrpcHttpResponse parseNonGrpcHttpResponse(String description) {
        if (description == null) {
            return null;
        }
        var matcher = NON_GRPC_HTTP_RESPONSE_PATTERN.matcher(description);
        return matcher.find() ? new NonGrpcHttpResponse(matcher.group(1), matcher.group(2)) : null;
    }

    private static boolean isTlsFailure(String description) {
        return description != null
                && (description.contains("SSLHandshakeException")
                    || description.contains("PKIX path building failed")
                    || description.contains("unable to find valid certification path"));
    }

    private static String summarizeDescription(String description) {
        if (description == null) {
            return null;
        }
        var oneLine = description.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        return oneLine.length() > 500 ? oneLine.substring(0, 500) + "..." : oneLine;
    }

    private static boolean isResponseReceived(Status.Code code) {
        return switch (code) {
            case DEADLINE_EXCEEDED, UNAVAILABLE, UNKNOWN, CANCELLED -> false;
            default -> true;
        };
    }

    private static String connectProxyTunnel(Socket socket, AviatorConnectionPlan connectionPlan, int timeoutSeconds) throws IOException {
        var proxy = connectionPlan.proxyDescriptor().orElseThrow();
        var target = connectionPlan.target().host() + ":" + connectionPlan.effectivePort();
        var writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1);
        writer.write("CONNECT " + target + " HTTP/1.1\r\n");
        writer.write("Host: " + target + "\r\n");
        if (proxy.getProxyUser() != null && proxy.getProxyPasswordAsString() != null) {
            var credentials = proxy.getProxyUser() + ":" + proxy.getProxyPasswordAsString();
            var encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            writer.write("Proxy-Authorization: Basic " + encoded + "\r\n");
        }
        writer.write("Proxy-Connection: Keep-Alive\r\n");
        writer.write("\r\n");
        writer.flush();

        socket.setSoTimeout(toMillis(timeoutSeconds));
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        var statusLine = reader.readLine();
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            throw new IOException("Proxy did not return an HTTP CONNECT response");
        }
        while (true) {
            var line = reader.readLine();
            if (line == null || line.isEmpty()) {
                break;
            }
        }
        if (!statusLine.matches("HTTP/\\d(?:\\.\\d)? 2\\d\\d.*")) {
            throw new IOException("Proxy CONNECT failed: " + statusLine);
        }
        return statusLine;
    }

    private static int toMillis(int timeoutSeconds) {
        return Math.max(1, timeoutSeconds) * 1000;
    }

    private record NonGrpcHttpResponse(String statusCode, String contentType) {}
}