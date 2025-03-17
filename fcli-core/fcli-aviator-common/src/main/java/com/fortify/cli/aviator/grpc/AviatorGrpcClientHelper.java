package com.fortify.cli.aviator.grpc;

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

    public static AviatorGrpcClient createClient(String url, IAviatorLogger logger) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String cleanUrl = url.replaceFirst("^[a-zA-Z]+://", "");
        String[] parts = cleanUrl.split(":");

        if (parts.length == 1) {
            LOG.debug("No port specified, using ManagedChannelBuilder.forTarget: {}", url);
            ManagedChannel channel = ManagedChannelBuilder.forTarget(url)
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
        } else {
            String host = parts[0].trim();
            if (host.isEmpty()) {
                throw new IllegalArgumentException("Host cannot be empty in URL: " + url);
            }

            try {
                int port = Integer.parseInt(parts[1].trim());
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
                throw new IllegalArgumentException("Invalid port in URL: " + url, e);
            }
        }
    }

    public static AviatorGrpcClient createClient(String url) {
        return createClient(url, null);
    }
}