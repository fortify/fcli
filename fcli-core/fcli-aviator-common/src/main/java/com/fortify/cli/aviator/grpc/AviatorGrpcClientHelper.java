/*
 * Copyright 2021-2025 Open Text.
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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AviatorGrpcClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClientHelper.class);

    public static AviatorGrpcClient createClient(String url, IAviatorLogger logger, long pingIntervalSeconds) throws AviatorSimpleException {
        if (url == null || url.trim().isEmpty()) {
            throw new AviatorSimpleException("Aviator URL cannot be null or empty.");
        }

        String cleanUrl = url.replaceFirst("^[a-zA-Z]+://", "");
        String[] parts = cleanUrl.split(":");

        if (parts.length == 1 && !cleanUrl.isEmpty()) {
            String target = cleanUrl;
            if (target.contains("/")) {
                String[] targetParts = target.split("/", 2);
                target = targetParts[0];
                LOG.warn("WARN: URL contained a path ('/'), using only the host part '{}' as target. Full URL: {}", target, url);
            }
            if (target.isEmpty()) {
                throw new AviatorSimpleException("Aviator URL is invalid: Host part is empty after cleaning. Provided URL: " + url);
            }

            LOG.debug("No port specified or using target string, using ManagedChannelBuilder.forTarget: {}", target);
            ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                    .useTransportSecurity()
                    .maxInboundMessageSize(16 * 1024 * 1024) // 16 MB
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .enableRetry()
                    .compressorRegistry(CompressorRegistry.getDefaultInstance())
                    .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                    .build();
            return new AviatorGrpcClient(channel, Constants.DEFAULT_TIMEOUT_SECONDS, logger, pingIntervalSeconds);

        } else if (parts.length == 2) {
            String host = parts[0].trim();
            String portStr = parts[1].trim();

            if (host.isEmpty()) {
                throw new AviatorSimpleException("Aviator URL is invalid: Host cannot be empty. Provided URL: " + url);
            }

            try {
                int port = Integer.parseInt(portStr);
                if (port <= 0 || port > 65535) {
                    throw new NumberFormatException("Port number out of range");
                }
                LOG.debug("Port specified, using ManagedChannelBuilder.forAddress: {}:{}", host, port);
                ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                        .useTransportSecurity()
                        .maxInboundMessageSize(16 * 1024 * 1024)
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(10, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .enableRetry()
                        .compressorRegistry(CompressorRegistry.getDefaultInstance())
                        .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                        .build();
                return new AviatorGrpcClient(channel, Constants.DEFAULT_TIMEOUT_SECONDS, logger, pingIntervalSeconds);
            } catch (NumberFormatException e) {
                throw new AviatorSimpleException("Aviator URL is invalid: Invalid port number '" + portStr + "'. Provided URL: " + url, e);
            }
        } else {
            throw new AviatorSimpleException("Aviator URL format is invalid. Expected 'host:port' or a valid target string. Provided URL: " + url);
        }
    }

    public static AviatorGrpcClient createClient(String url) throws AviatorSimpleException {
        return createClient(url, null, Constants.DEFAULT_PING_INTERVAL_SECONDS);
    }
}