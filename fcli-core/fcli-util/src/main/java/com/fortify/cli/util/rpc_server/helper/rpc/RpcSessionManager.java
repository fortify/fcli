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
package com.fortify.cli.util.rpc_server.helper.rpc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages sessions for the RPC server. This class:
 * - Creates unique session names for each product type (SSC, FoD, etc.)
 * - Tracks which sessions have been created by the RPC server
 * - Auto-discovers which session type is needed for a command
 * - Provides session options to be added to commands
 * - Logs out all sessions when the server shuts down
 * 
 * The architecture is extensible: new products can be added by registering
 * additional product handlers.
 * 
 * @author Ruud Senden
 */
@Slf4j
public final class RpcSessionManager {
    
    /**
     * Supported product types and their session option names.
     */
    public enum ProductType {
        SSC("--ssc-session", "ssc", "ssc session"),
        FOD("--fod-session", "fod", "fod session"),
        SC_SAST("--ssc-session", "sc-sast", "ssc session"),  // SC-SAST uses SSC session
        SC_DAST("--ssc-session", "sc-dast", "ssc session");  // SC-DAST uses SSC session
        
        @Getter private final String sessionOption;
        @Getter private final String commandPrefix;
        @Getter private final String sessionCommandPrefix;
        
        ProductType(String sessionOption, String commandPrefix, String sessionCommandPrefix) {
            this.sessionOption = sessionOption;
            this.commandPrefix = commandPrefix;
            this.sessionCommandPrefix = sessionCommandPrefix;
        }
        
        /**
         * Determine the product type from a command string.
         */
        public static ProductType fromCommand(String command) {
            if (command == null) return null;
            var normalizedCmd = command.toLowerCase().replaceFirst("^fcli\\s+", "").trim();
            
            // Check specific product prefixes
            if (normalizedCmd.startsWith("ssc ")) return SSC;
            if (normalizedCmd.startsWith("fod ")) return FOD;
            if (normalizedCmd.startsWith("sc-sast ")) return SC_SAST;
            if (normalizedCmd.startsWith("sc-dast ")) return SC_DAST;
            
            return null;
        }
        
        /**
         * Get the actual session type for this product (e.g., SC-SAST uses SSC session).
         */
        public ProductType getSessionType() {
            return switch (this) {
                case SC_SAST, SC_DAST -> SSC;
                default -> this;
            };
        }
    }
    
    private final ObjectMapper objectMapper;
    
    // Unique ID for this RPC server instance
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);
    
    // Session names created by this RPC server (product type -> session name)
    private final Map<ProductType, String> sessionNames = new HashMap<>();
    
    // Set of sessions that we've successfully logged in (need to logout on shutdown)
    private final Set<ProductType> activeSessions = new LinkedHashSet<>();
    
    // Registry of RPC method handlers for session login (product -> handler)
    private final Map<String, IRpcMethodHandler> loginHandlers = new LinkedHashMap<>();
    
    // Registry of RPC method handlers for session logout (product -> handler)
    private final Map<String, IRpcMethodHandler> logoutHandlers = new LinkedHashMap<>();
    
    public RpcSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        registerDefaultHandlers();
    }
    
    private void registerDefaultHandlers() {
        // Register SSC session handlers
        registerLoginHandler("ssc", new RpcMethodHandlerSscLogin(objectMapper, this));
        registerLogoutHandler("ssc", new RpcMethodHandlerSscLogout(objectMapper, this));
        
        // Register FoD session handlers
        registerLoginHandler("fod", new RpcMethodHandlerFodLogin(objectMapper, this));
        registerLogoutHandler("fod", new RpcMethodHandlerFodLogout(objectMapper, this));
    }
    
    /**
     * Register a login handler for a product.
     */
    public void registerLoginHandler(String product, IRpcMethodHandler handler) {
        loginHandlers.put(product.toLowerCase(), handler);
    }
    
    /**
     * Register a logout handler for a product.
     */
    public void registerLogoutHandler(String product, IRpcMethodHandler handler) {
        logoutHandlers.put(product.toLowerCase(), handler);
    }
    
    /**
     * Get all login handlers (for registering RPC methods).
     */
    public Map<String, IRpcMethodHandler> getLoginHandlers() {
        return Map.copyOf(loginHandlers);
    }
    
    /**
     * Get all logout handlers (for registering RPC methods).
     */
    public Map<String, IRpcMethodHandler> getLogoutHandlers() {
        return Map.copyOf(logoutHandlers);
    }
    
    /**
     * Get the session name for a product type, creating one if needed.
     */
    public String getSessionName(ProductType productType) {
        // Use the actual session type (e.g., SC-SAST uses SSC session)
        var sessionType = productType.getSessionType();
        return sessionNames.computeIfAbsent(sessionType, 
            pt -> "rpc-" + instanceId + "-" + pt.name().toLowerCase());
    }
    
    /**
     * Get session options to add to a command, based on the command prefix.
     * Returns empty map if the command doesn't need a session or if we don't have one.
     */
    public Map<String, String> getSessionOptionsForCommand(String command) {
        var productType = ProductType.fromCommand(command);
        if (productType == null) {
            return Map.of();
        }
        
        // Use the actual session type
        var sessionType = productType.getSessionType();
        
        // If we have an active session for this product type, add the option
        if (activeSessions.contains(sessionType)) {
            var sessionName = sessionNames.get(sessionType);
            if (sessionName != null) {
                return Map.of(productType.getSessionOption(), sessionName);
            }
        }
        
        return Map.of();
    }
    
    /**
     * Execute login command and track the session.
     */
    public JsonNode executeLogin(ProductType productType, String loginArgs) {
        var sessionName = getSessionName(productType);
        var loginCmd = buildLoginCommand(productType, sessionName, loginArgs);
        
        log.info("RPC session login: {} (session: {})", productType, sessionName);
        
        var result = FcliCommandExecutorFactory.builder()
            .cmd(loginCmd)
            .stdoutOutputType(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .onFail(r -> {})
            .build().create().execute();
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("product", productType.name().toLowerCase().replace("_", "-"));
        response.put("sessionName", sessionName);
        
        if (result.getExitCode() == 0) {
            activeSessions.add(productType.getSessionType());
            response.put("success", true);
            response.put("message", "Successfully logged in to " + productType);
            log.info("RPC session login successful: {}", sessionName);
        } else {
            response.put("success", false);
            response.put("message", "Login failed: " + result.getErr());
            response.put("stderr", result.getErr());
            log.error("RPC session login failed: {} - {}", sessionName, result.getErr());
        }
        
        return response;
    }
    
    /**
     * Execute logout command for a product.
     */
    public JsonNode executeLogout(ProductType productType) {
        var sessionType = productType.getSessionType();
        var sessionName = sessionNames.get(sessionType);
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("product", productType.name().toLowerCase().replace("_", "-"));
        
        if (sessionName == null || !activeSessions.contains(sessionType)) {
            response.put("success", true);
            response.put("message", "No active session to logout");
            return response;
        }
        
        var logoutCmd = buildLogoutCommand(productType, sessionName);
        
        log.info("RPC session logout: {} (session: {})", productType, sessionName);
        
        var result = FcliCommandExecutorFactory.builder()
            .cmd(logoutCmd)
            .stdoutOutputType(OutputType.suppress)
            .stderrOutputType(OutputType.collect)
            .onFail(r -> {})
            .build().create().execute();
        
        response.put("sessionName", sessionName);
        
        if (result.getExitCode() == 0) {
            activeSessions.remove(sessionType);
            response.put("success", true);
            response.put("message", "Successfully logged out from " + productType);
            log.info("RPC session logout successful: {}", sessionName);
        } else {
            response.put("success", false);
            response.put("message", "Logout failed: " + result.getErr());
            log.warn("RPC session logout failed: {} - {}", sessionName, result.getErr());
        }
        
        return response;
    }
    
    /**
     * Logout from all sessions created by this RPC server.
     * Called on server shutdown.
     */
    public void logoutAll() {
        log.info("Logging out all RPC sessions...");
        
        // Iterate through activeSessions directly to avoid duplicate logout attempts
        // (e.g., SC_SAST and SC_DAST share SSC session type)
        for (var sessionType : Set.copyOf(activeSessions)) {
            try {
                executeLogout(sessionType);
            } catch (Exception e) {
                log.warn("Failed to logout session for {}: {}", sessionType, e.getMessage());
            }
        }
        
        activeSessions.clear();
        sessionNames.clear();
        log.info("All RPC sessions logged out");
    }
    
    /**
     * Get list of active sessions as JSON.
     */
    public JsonNode getActiveSessions() {
        ArrayNode sessions = objectMapper.createArrayNode();
        for (var productType : activeSessions) {
            ObjectNode session = objectMapper.createObjectNode();
            session.put("product", productType.name().toLowerCase().replace("_", "-"));
            session.put("sessionName", sessionNames.get(productType));
            sessions.add(session);
        }
        return sessions;
    }
    
    /**
     * Check if a session is active for a product type.
     */
    public boolean hasActiveSession(ProductType productType) {
        return activeSessions.contains(productType.getSessionType());
    }
    
    private String buildLoginCommand(ProductType productType, String sessionName, String loginArgs) {
        // Session name is generated internally (rpc-{uuid}-{product}) and is safe
        // loginArgs are pre-quoted by the login handlers
        var baseCmd = productType.getSessionCommandPrefix() + " login";
        return String.format("%s %s %s", baseCmd, sessionName, loginArgs != null ? loginArgs : "").trim();
    }
    
    private String buildLogoutCommand(ProductType productType, String sessionName) {
        // Session name is generated internally and is safe
        var baseCmd = productType.getSessionCommandPrefix() + " logout";
        return String.format("%s %s", baseCmd, sessionName);
    }
}
