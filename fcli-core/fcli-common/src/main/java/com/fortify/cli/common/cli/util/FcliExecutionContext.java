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
/*
 * Copyright 2021-2026 Open Text.
 */
package com.fortify.cli.common.cli.util;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.rest.unirest.UnirestContext;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

import lombok.Getter;

/**
 * Per-top-level execution context holding mutable execution-scoped state.
 * The {@code globalActionValues} ObjectNode is backed by a {@link ConcurrentHashMap}
 * to allow safe concurrent access from multiple threads (e.g. async jobs,
 * server request handlers).
 */
public final class FcliExecutionContext {
    @Getter private final ObjectNode globalActionValues = new ObjectNode(JsonNodeFactory.instance, new ConcurrentHashMap<>());
    @Getter private final UnirestContext unirestContext = new UnirestContext();
    @Getter private volatile String mcpRequestAuthScopeKey;
    // Encryption helper used for encrypt/decrypt in this execution. Default to global DEFAULT.
    private volatile EncryptionHelper encryptionHelper = EncryptionHelper.DEFAULT;
    // Set of absolute file paths that were saved using ephemeral encryption during this execution
    private final Set<Path> ephemeralEncryptedFiles = ConcurrentHashMap.newKeySet();
    @Getter private final Map<String, ISessionDescriptor> transientSessionDescriptors = new ConcurrentHashMap<>();

    public void clearTransientSessionDescriptors() {
        transientSessionDescriptors.clear();
    }

    public ISessionDescriptor getTransientSessionDescriptor(String type) {
        return type == null ? null : transientSessionDescriptors.get(type);
    }

    public void setTransientSessionDescriptor(ISessionDescriptor descriptor) {
        if ( descriptor == null ) {
            return;
        }
        transientSessionDescriptors.put(descriptor.getType(), descriptor);
    }

    public void clearTransientSessionDescriptor(String type) {
        if ( type != null ) {
            transientSessionDescriptors.remove(type);
        }
    }

    public void setMcpRequestAuthScopeKey(String mcpRequestAuthScopeKey) {
        this.mcpRequestAuthScopeKey = mcpRequestAuthScopeKey;
    }

    public String info() {
        return String.format("FcliExecutionContext@%s(%d) actionGlobalValues@%s(%d) unirestContext@%s(%s) transientSessions=%d authScope=%s",
                Integer.toHexString(System.identityHashCode(this)),
                FcliExecutionContextHolder.stackDepth(),
                Integer.toHexString(System.identityHashCode(globalActionValues)),
                globalActionValues.size(),
                Integer.toHexString(System.identityHashCode(unirestContext)),
                unirestContext.getCachedInstanceCount(),
                transientSessionDescriptors.size(),
                mcpRequestAuthScopeKey != null ? "set" : "unset");
    }

    /**
     * Enable ephemeral encryption for this execution. If already enabled, this is a no-op.
     * Generates a secure random password and configures an EncryptionHelper instance for this context.
     * @return true if ephemeral encryption is enabled (either already or newly)
     */
    public boolean enableEphemeralEncryption() {
        if ( encryptionHelper!=EncryptionHelper.DEFAULT ) { return true; }
        synchronized(this) {
            var rnd = new byte[32];
            new SecureRandom().nextBytes(rnd);
            String pwd = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
            encryptionHelper = new EncryptionHelper(pwd);
        }
        return true;
    }

    public boolean isEphemeralEncryptionEnabled() { return encryptionHelper!=EncryptionHelper.DEFAULT; }
    public EncryptionHelper getEncryptionHelper() { return encryptionHelper; }

    public void registerEphemeralEncryptedFile(Path absolutePath) { if ( absolutePath!=null ) { ephemeralEncryptedFiles.add(absolutePath); } }
    public boolean isEphemeralEncryptedFile(Path absolutePath) { return absolutePath!=null && ephemeralEncryptedFiles.contains(absolutePath); }

    /**
     * Save encrypted contents to the given absolute path using the appropriate EncryptionHelper for this context.
     * If ephemeral encryption is enabled for this context, the file will be registered as ephemeral-encrypted.
     */
    public void saveEncrypted(Path absolutePath, String plainContents) {
        // Use the context's configured helper (either DEFAULT or ephemeral instance)
        encryptionHelper.save(plainContents, absolutePath);
        if ( isEphemeralEncryptionEnabled() ) { registerEphemeralEncryptedFile(absolutePath); }
    }

    /**
     * Read and decrypt the contents of the given absolute path using the appropriate EncryptionHelper for this path.
     */
    public String readEncrypted(Path absolutePath) {
        if ( absolutePath==null ) { return null; }
        var helper = isEphemeralEncryptedFile(absolutePath) ? getEncryptionHelper() : EncryptionHelper.DEFAULT;
        return helper.read(absolutePath);
    }
}
