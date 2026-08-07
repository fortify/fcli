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
package com.fortify.cli.aviator.grpc;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.UnsupportedAviatorUrlSchemeException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.http.UrlSchemes;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.http.ssl.trust.FcliTrustManager;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.HttpConnectProxiedSocketAddress;
import io.grpc.ProxiedSocketAddress;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;

public class AviatorGrpcClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClientHelper.class);
    private static final String AVIATOR_MODULE = "aviator";

    public static AviatorGrpcClient createClient(String url, IAviatorLogger logger, long pingIntervalSeconds) throws AviatorSimpleException {
        if (url == null || url.trim().isEmpty()) {
            throw new AviatorSimpleException("Aviator URL cannot be null or empty.");
        }

        var connectionPlan = createConnectionPlan(url);
        var proxyDescriptor = connectionPlan.proxyDescriptor();
        var target = connectionPlan.target();
        var builder = target.port() == null
                ? NettyChannelBuilder.forTarget(target.host())
                : NettyChannelBuilder.forAddress(target.host(), target.port());

        if ( target.port()==null ) {
            LOG.debug("No explicit port configured, using NettyChannelBuilder.forTarget: {}", target.host());
        } else {
            LOG.debug("Port specified, using NettyChannelBuilder.forAddress: {}:{}", target.host(), target.port());
        }

        var channel = configureBuilder(builder, proxyDescriptor).build();
        return new AviatorGrpcClient(channel, Constants.DEFAULT_TIMEOUT_SECONDS, logger, pingIntervalSeconds);
    }

    public static AviatorConnectionPlan createConnectionPlan(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new AviatorSimpleException("Aviator URL cannot be null or empty.");
        }
        var normalizedUrl = normalizeUrl(url);
        var target = parseTarget(url);
        var proxyDescriptor = ProxyHelper.getProxyDescriptorOrEnv(AVIATOR_MODULE, normalizedUrl);
        var effectivePort = target.port() == null ? 443 : target.port();
        return new AviatorConnectionPlan(url, normalizedUrl, target, effectivePort, proxyDescriptor);
    }

    public static ParsedTarget parseTarget(String url) {
        var normalizedUrl = normalizeUrl(url);
        URI uri;
        try {
            uri = URI.create(normalizedUrl);
        } catch (Exception e) {
            throw new AviatorSimpleException("Aviator URL format is invalid. Expected 'host:port' or a valid target string. Provided URL: " + url, e);
        }

        var host = uri.getHost();
        if ( host==null || host.isBlank() ) {
            throw new AviatorSimpleException("Aviator URL is invalid: Host cannot be empty. Provided URL: " + url);
        }

        if ( uri.getPath()!=null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()) ) {
            LOG.warn("WARN: URL contained a path ('{}'), using only the host/port part. Full URL: {}", uri.getPath(), url);
        }

        var port = uri.getPort();
        if ( port==-1 ) {
            return new ParsedTarget(host, null);
        } else if (port <= 0 || port > 65535) {
            throw new AviatorSimpleException("Aviator URL is invalid: Invalid port number '"+port+"'. Provided URL: " + url);
        } else {
            return new ParsedTarget(host, port);
        }
    }

    /**
     * Normalizes an Aviator target to an https URL.
     * <ul>
     *   <li>Scheme-less input ({@code host} or {@code host:port}) gets {@code https://} prepended.</li>
     *   <li>{@code https} (any casing) is accepted and canonicalized to lowercase {@code https}.</li>
     *   <li>Any other scheme ({@code http}, {@code ftp}, typos like {@code mttp}, …) is rejected.</li>
     * </ul>
     */
    public static String normalizeUrl(String url) {
        var trimmed = url.trim();
        if ( !UrlSchemes.hasScheme(trimmed) ) {
            return "https://"+trimmed;
        }
        var schemeSeparator = trimmed.indexOf("://");
        var scheme = trimmed.substring(0, schemeSeparator);
        if ( !"https".equalsIgnoreCase(scheme) ) {
            throw new UnsupportedAviatorUrlSchemeException(scheme, url);
        }
        if ( "https".equals(scheme) ) {
            return trimmed;
        }
        // Canonicalize scheme casing (HTTPS://host → https://host)
        return "https"+trimmed.substring(schemeSeparator);
    }

    private static NettyChannelBuilder configureBuilder(NettyChannelBuilder builder, Optional<ProxyDescriptor> proxyDescriptor) {
        var configuredBuilder = builder
            .sslContext(createSslContext())
            .maxInboundMessageSize(16 * 1024 * 1024)
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .enableRetry()
            .compressorRegistry(CompressorRegistry.getDefaultInstance())
            .decompressorRegistry(DecompressorRegistry.getDefaultInstance());

        proxyDescriptor.ifPresent(d->configuredBuilder.proxyDetector(targetAddress->toProxiedSocketAddress(targetAddress, d)));
        return configuredBuilder;
    }

    private static SslContext createSslContext() {
        try {
            FcliTrustManager.refreshIfChanged();
            return GrpcSslContexts.forClient()
                .trustManager(FcliTrustManager.getInstance())
                .build();
        } catch (SSLException e) {
            throw new AviatorSimpleException("Unable to initialize Aviator gRPC TLS context", e);
        }
    }

    static ProxiedSocketAddress toProxiedSocketAddress(SocketAddress targetAddress, ProxyDescriptor proxyDescriptor) {
        if ( !(targetAddress instanceof InetSocketAddress inetSocketAddress) ) {
            return null;
        }

        var builder = HttpConnectProxiedSocketAddress.newBuilder()
            .setTargetAddress(inetSocketAddress)
            .setProxyAddress(new InetSocketAddress(proxyDescriptor.getProxyHost(), proxyDescriptor.getProxyPort()));

        if ( proxyDescriptor.getProxyUser()!=null ) { builder.setUsername(proxyDescriptor.getProxyUser()); }
        var proxyPassword = proxyDescriptor.getProxyPasswordAsString();
        if ( proxyPassword!=null ) { builder.setPassword(proxyPassword); }
        return builder.build();
    }

    public static AviatorGrpcClient createClient(String url) throws AviatorSimpleException {
        return createClient(url, null, Constants.DEFAULT_PING_INTERVAL_SECONDS);
    }

    public record AviatorConnectionPlan(
            String originalUrl,
            String normalizedUrl,
            ParsedTarget target,
            int effectivePort,
            Optional<ProxyDescriptor> proxyDescriptor) {}

    public record ParsedTarget(String host, Integer port) {}
}