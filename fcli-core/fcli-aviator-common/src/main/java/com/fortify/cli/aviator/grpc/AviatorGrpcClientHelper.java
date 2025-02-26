package com.fortify.cli.aviator.grpc;

import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AviatorGrpcClientHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClientHelper.class);
    private static final int DEFAULT_PORT = 9090;

    public static AviatorGrpcClient createClient(String url) {
        try{
            String[] parts = url.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_PORT;
            LOG.debug("Creating gRPC client for host: {}, port: {}", host, port);
            return new AviatorGrpcClient(host, port, 10, null);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port in URL: " + url, e);
        }
    }
}