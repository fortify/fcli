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
package com.fortify.cli.common.cli.util;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.log.LogMaskContext;
import com.fortify.cli.common.rest.unirest.UnirestContext;

import lombok.Getter;

/**
 * Per-invocation execution frame holding the three components of execution state:
 *
 * <ul>
 *   <li><b>{@link UnirestContext}</b> — always fresh for every external entry point (plain CLI
 *       command, MCP tool call, RPC method call). A fresh context ensures that Unirest
 *       connection pools created in one invocation are shut down before the next one begins,
 *       so that any session changes made between invocations (login/logout, credential rotation,
 *       URL change) are always picked up rather than silently reused from a stale connection.
 *       Inner sub-commands executed via {@code run.fcli} reuse the parent's UnirestContext
 *       because they run within the same execution frame.</li>
 *
 *   <li><b>{@link FcliActionState}</b> — holds {@code global.*} action variables. Isolation
 *       rules vary by call site:
 *       <ul>
 *         <li>Each external CLI invocation and each top-level MCP/RPC tool call starts with a
 *             fresh {@code FcliActionState}, so {@code global.*} variables never leak between
 *             independent calls.</li>
         *         <li>Imported functions in MCP stdio / RPC stdio share the same {@code FcliActionState}
 *             across all calls within the lifetime of the same server instance, so that one function
 *             can set a variable that a later function call reads back.</li>
 *         <li>Imported functions in the MCP HTTP server use a per-credentials-hash
 *             {@code FcliActionState} stored in the per-auth-scope {@link FcliIsolationScope},
 *             so {@code global.*} variables persist across calls from the same authenticated
 *             identity but are isolated from other users.</li>
 *         <li>Inner action invocations triggered via {@code run.fcli} inherit the parent frame's
 *             {@code FcliActionState}, giving them read/write access to the same
 *             {@code global.*} map as their caller.</li>
 *       </ul></li>
 *
 *   <li><b>{@link FcliIsolationScope}</b> — groups related invocations that share the same
 *       auth/session boundary. See {@link FcliIsolationScope} for details.
 * </ul>
 *
 * <p>The preferred way to manage context lifetime is through
 * {@link FcliExecutionContextHolder#push} / {@link FcliExecutionContextHolder#pushNew},
 * which return a {@link FcliExecutionContextHolder.ContextFrame} that can be used with
 * try-with-resources. Closing the frame pops the context from the stack and calls
 * {@link #close()}, which shuts down the owned {@link UnirestContext}.</p>
 */
public final class FcliExecutionContext implements AutoCloseable {
    @Getter private final FcliIsolationScope isolationScope;
    @Getter private final FcliActionState actionState;
    @Getter private final LogMaskContext logMaskContext;
    @Getter private final UnirestContext unirestContext = new UnirestContext();
    // Encryption helper used for encrypt/decrypt in this execution. Default to global DEFAULT.
    private volatile EncryptionHelper encryptionHelper = EncryptionHelper.DEFAULT;
    // Set of absolute file paths that were saved using ephemeral encryption during this execution
    private final Set<Path> ephemeralEncryptedFiles = ConcurrentHashMap.newKeySet();

    public FcliExecutionContext() {
        this(new FcliIsolationScope(), new FcliActionState(), new LogMaskContext());
    }

    public FcliExecutionContext(FcliIsolationScope isolationScope, FcliActionState actionState) {
        this(isolationScope, actionState, new LogMaskContext());
    }

    public FcliExecutionContext(FcliIsolationScope isolationScope, FcliActionState actionState, LogMaskContext logMaskContext) {
        this.isolationScope = Objects.requireNonNull(isolationScope, "isolationScope");
        this.actionState = Objects.requireNonNull(actionState, "actionState");
        this.logMaskContext = Objects.requireNonNull(logMaskContext, "logMaskContext");
    }

    /**
     * Create a child execution frame that inherits the current isolation scope but
     * starts with a <em>fresh</em> {@link FcliActionState}.
     *
     * <p>Used when the child must share the same auth/session boundary as its parent
     * (e.g. worker threads in {@code MCPJobManager} and {@code AsyncJobManager}) but
     * must not see or mutate the parent's {@code global.*} action variables.</p>
     */
    public FcliExecutionContext createChild() {
        return new FcliExecutionContext(isolationScope, new FcliActionState(), logMaskContext);
    }

    /**
     * Releases resources held by this execution context, specifically shutting down
     * all cached {@link UnirestContext} connections. Should be called by the entry
     * point that pushed this context once execution is complete.
     */
    @Override
    public void close() {
        unirestContext.close();
    }

    public String info() {
        return String.format("FcliExecutionContext@%s(%d) isolationScope@%s actionState@%s actionGlobalValues@%s(%d) unirestContext@%s(%s) transientSessions=%d authScope=%s",
                Integer.toHexString(System.identityHashCode(this)),
                FcliExecutionContextHolder.stackDepth(),
                Integer.toHexString(System.identityHashCode(isolationScope)),
                Integer.toHexString(System.identityHashCode(actionState)),
                Integer.toHexString(System.identityHashCode(actionState.getGlobalActionValues())),
                actionState.getGlobalActionValues().size(),
                Integer.toHexString(System.identityHashCode(unirestContext)),
                unirestContext.getCachedInstanceCount(),
                isolationScope.getTransientSessionDescriptors().size(),
                isolationScope.getMcpRequestAuthScopeKey() != null ? "set" : "unset");
    }

    /**
     * Enable ephemeral encryption for this execution. If already enabled, this is a no-op.
     * Generates a secure random password and configures an EncryptionHelper instance for this context.
     * @return true if ephemeral encryption is enabled (either already or newly)
     */
    public boolean enableEphemeralEncryption() {
        if ( encryptionHelper!=EncryptionHelper.DEFAULT ) { return true; }
        synchronized(this) {
            if ( encryptionHelper!=EncryptionHelper.DEFAULT ) { return true; }
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
