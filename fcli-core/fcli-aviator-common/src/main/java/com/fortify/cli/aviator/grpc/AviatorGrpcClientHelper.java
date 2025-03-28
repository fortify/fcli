package com.fortify.cli.aviator.grpc;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException; // Added import
import com.fortify.cli.aviator.config.IAviatorLogger;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class AviatorGrpcClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClientHelper.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public static AviatorGrpcClient createClient(String url, IAviatorLogger logger) throws AviatorSimpleException {
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
                LOG.warn("URL contained a path ('/'), using only the host part '{}' as target. Full URL: {}", target, url);
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
            return new AviatorGrpcClient(channel, DEFAULT_TIMEOUT_SECONDS, logger);

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
                return new AviatorGrpcClient(channel, DEFAULT_TIMEOUT_SECONDS, logger);
            } catch (NumberFormatException e) {
                throw new AviatorSimpleException("Aviator URL is invalid: Invalid port number '" + portStr + "'. Provided URL: " + url, e);
            }
        } else {
            throw new AviatorSimpleException("Aviator URL format is invalid. Expected 'host:port' or a valid target string. Provided URL: " + url);
        }
    }

    public static AviatorGrpcClient createClient(String url) throws AviatorSimpleException {
        return createClient(url, null);
    }
}