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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.common.http.ssl.trust.FcliTrustManager;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class AviatorDefaultDiagnosticProbe implements IAviatorDiagnosticProbe {
    private static final Pattern NON_GRPC_HTTP_RESPONSE_PATTERN = Pattern.compile(
        "HTTP status code (\\d+) invalid content-type: ([^\\s;]+(?:;\\s*[^\\s]+)?)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_CONNECT_HEADER_BYTES = 64 * 1024;

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
    public AviatorTunnelResult probeTunnel(AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        var proxyConfigured = connectionPlan.proxyDescriptor().isPresent();
        try (var rawSocket = new Socket()) {
            var opened = openTunnelSocket(rawSocket, connectionPlan, timeoutSeconds, proxyConfigured);
            if (opened instanceof OpenFailed failed) {
                return failed.result();
            }
            var openedOk = (OpenedSocket) opened;
            return handshakeTls(rawSocket, connectionPlan, timeoutSeconds, proxyConfigured, openedOk.proxyConnectStatus());
        } catch (Exception e) {
            return new AviatorTunnelResult.TlsFailed(proxyConfigured, "not-used", AviatorTlsPhase.CONNECT, e);
        }
    }

    private OpenResult openTunnelSocket(Socket rawSocket, AviatorConnectionPlan connectionPlan, int timeoutSeconds,
            boolean proxyConfigured) {
        try {
            if (proxyConfigured) {
                var proxy = connectionPlan.proxyDescriptor().get();
                rawSocket.connect(new InetSocketAddress(proxy.getProxyHost(), proxy.getProxyPort()), toMillis(timeoutSeconds));
                rawSocket.setSoTimeout(toMillis(timeoutSeconds));
                var status = performProxyConnect(rawSocket, connectionPlan);
                return new OpenedSocket(status);
            }
            rawSocket.connect(new InetSocketAddress(connectionPlan.target().host(), connectionPlan.effectivePort()),
                toMillis(timeoutSeconds));
            rawSocket.setSoTimeout(toMillis(timeoutSeconds));
            return new OpenedSocket("not-used");
        } catch (AviatorProxyConnectException e) {
            return new OpenFailed(new AviatorTunnelResult.ProxyConnectFailed(e));
        } catch (Exception e) {
            return new OpenFailed(new AviatorTunnelResult.TlsFailed(proxyConfigured, "not-used", AviatorTlsPhase.CONNECT, e));
        }
    }

    private AviatorTunnelResult handshakeTls(Socket rawSocket, AviatorConnectionPlan connectionPlan, int timeoutSeconds,
            boolean proxyConfigured, String proxyConnectStatus) {
        try {
            var sslSocketFactory = createSslSocketFactory();
            try (var sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                rawSocket, connectionPlan.target().host(), connectionPlan.effectivePort(), true)) {
                sslSocket.setSoTimeout(toMillis(timeoutSeconds));
                applyTlsParameters(sslSocket, connectionPlan.target().host());
                sslSocket.startHandshake();
                return toTlsSucceeded(sslSocket, proxyConfigured, proxyConnectStatus);
            }
        } catch (Exception e) {
            var phase = AviatorTlsFailureDetector.isTlsFailure(e) ? AviatorTlsPhase.HANDSHAKE : AviatorTlsPhase.CONNECT;
            return new AviatorTunnelResult.TlsFailed(proxyConfigured, proxyConnectStatus, phase, e);
        }
    }

    private static void applyTlsParameters(SSLSocket sslSocket, String host) {
        SSLParameters parameters = sslSocket.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        parameters.setServerNames(List.of(new SNIHostName(host)));
        parameters.setApplicationProtocols(new String[] {"h2"});
        sslSocket.setSSLParameters(parameters);
    }

    private static AviatorTunnelResult.TlsSucceeded toTlsSucceeded(SSLSocket sslSocket, boolean proxyConfigured,
            String proxyConnectStatus) throws Exception {
        var session = sslSocket.getSession();
        var certificates = session.getPeerCertificates();
        var peerSubject = certificates.length > 0 && certificates[0] instanceof X509Certificate certificate
                ? certificate.getSubjectX500Principal().getName()
                : "unknown";
        return new AviatorTunnelResult.TlsSucceeded(
            proxyConfigured, proxyConnectStatus, session.getProtocol(), session.getCipherSuite(),
            peerSubject, sslSocket.getApplicationProtocol());
    }

    @Override
    public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) throws Exception {
        try (var client = AviatorGrpcClientHelper.createClient(url)) {
            client.probeGetDefaultQuota(timeoutSeconds);
            return AviatorGrpcReachabilityResult.ok("OK", "Default quota response received");
        } catch (StatusRuntimeException e) {
            var status = e.getStatus();
            var responseReceived = isResponseReceived(status.getCode());
            var description = summarizeDescription(status.getDescription());
            var httpResponse = parseNonGrpcHttpResponse(description);
            if (httpResponse != null) {
                return AviatorGrpcReachabilityResult.nonGrpcHttp(status.getCode().name(),
                    httpResponse.statusCode(), httpResponse.contentType(), description);
            }
            var tlsFailure = AviatorTlsFailureDetector.isTlsFailure(e, description)
                    || AviatorTlsFailureDetector.isTlsFailure(status.getCause(), description);
            if (responseReceived) {
                return AviatorGrpcReachabilityResult.ok(status.getCode().name(), description);
            }
            var category = tlsFailure ? AviatorGrpcFailureCategory.TLS : AviatorGrpcFailureCategory.NO_RESPONSE;
            return AviatorGrpcReachabilityResult.noResponse(status.getCode().name(), category, description);
        }
    }

    private static SSLSocketFactory createSslSocketFactory() throws IOException {
        try {
            FcliTrustManager.refreshIfChanged();
            var context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {FcliTrustManager.getInstance()}, null);
            return context.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to initialize TLS trust context for Aviator diagnostics", e);
        }
    }

    static String performProxyConnect(Socket socket, AviatorConnectionPlan connectionPlan) throws IOException {
        var proxy = connectionPlan.proxyDescriptor()
            .orElseThrow(() -> new AviatorBugException("performProxyConnect requires a proxy on the connection plan"));
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

        var statusLine = readHttpHeaders(socket.getInputStream());
        validateProxyConnectStatusLine(statusLine);
        return statusLine;
    }

    /**
     * Validates the CONNECT status line after headers are read. Package-visible for unit tests.
     */
    static void validateProxyConnectStatusLine(String statusLine) throws AviatorProxyConnectException {
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            throw new AviatorProxyConnectException("Proxy did not return an HTTP CONNECT response");
        }
        if (!statusLine.matches("HTTP/\\d(?:\\.\\d)? 2\\d\\d.*")) {
            throw new AviatorProxyConnectException("Proxy CONNECT failed: " + statusLine);
        }
    }

    static String readHttpHeaders(InputStream input) throws IOException {
        var buffer = new ByteArrayOutputStream();
        var match = 0;
        final byte[] end = new byte[] {'\r', '\n', '\r', '\n'};
        while (match < end.length) {
            var value = input.read();
            if (value < 0) {
                break;
            }
            buffer.write(value);
            if (buffer.size() > MAX_CONNECT_HEADER_BYTES) {
                throw new AviatorProxyConnectException(
                    "Proxy CONNECT response headers exceeded " + MAX_CONNECT_HEADER_BYTES + " bytes");
            }
            match = value == end[match] ? match + 1 : (value == end[0] ? 1 : 0);
        }
        var headers = buffer.toString(StandardCharsets.ISO_8859_1);
        var lineEnd = headers.indexOf("\r\n");
        return lineEnd < 0 ? headers.trim() : headers.substring(0, lineEnd);
    }

    private static NonGrpcHttpResponse parseNonGrpcHttpResponse(String description) {
        if (description == null) {
            return null;
        }
        var matcher = NON_GRPC_HTTP_RESPONSE_PATTERN.matcher(description);
        return matcher.find() ? new NonGrpcHttpResponse(matcher.group(1), matcher.group(2)) : null;
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

    private static int toMillis(int timeoutSeconds) {
        return Math.max(1, timeoutSeconds) * 1000;
    }

    private sealed interface OpenResult {}
    private record OpenedSocket(String proxyConnectStatus) implements OpenResult {}
    private record OpenFailed(AviatorTunnelResult result) implements OpenResult {}

    private record NonGrpcHttpResponse(String statusCode, String contentType) {}
}
