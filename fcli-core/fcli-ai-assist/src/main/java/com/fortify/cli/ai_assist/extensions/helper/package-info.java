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
/**
 * AI assistant extensions: installation, update, and management.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link AiAssistExtensionsHelper} — Facade: setup, uninstall, list operations</li>
 * </ul>
 *
 * <h2>Internal helpers (package-private)</h2>
 * <ul>
 *   <li>{@link AiAssistExtensionsInstallPlanHelper} — Builds and executes diff-based install plans</li>
 *   <li>{@link AiAssistExtensionsContentHelper} — Discovers source files and computes target paths</li>
 *   <li>{@link AiAssistExtensionsStateHelper} — Persistent state: manifests, installations registry, file I/O</li>
 *   <li>{@link AiAssistExtensionsConditionEvaluator} — Evaluates platform/tool conditions for auto-detection</li>
 *   <li>{@link AiAssistExtensionsPathResolver} — Resolves target directory paths with platform/env expansion</li>
 *   <li>{@link AiAssistExtensionsSourceHandler} — Downloads, extracts, and reads extension archives</li>
 * </ul>
 *
 * <h2>Descriptors</h2>
 * <ul>
 *   <li>{@link AiAssistExtensionsDistributionDescriptor} — Distribution manifest (assistants + targets)</li>
 *   <li>{@link AiAssistExtensionsContentManifestDescriptor} — Content manifest (content types + discovery config)</li>
 *   <li>{@link AiAssistExtensionsContentTypeDescriptor} — Per-content-type discovery configuration</li>
 *   <li>{@link AiAssistExtensionsAssistantDescriptor} — Assistant definition (name, targets, conditions)</li>
 *   <li>{@link AiAssistExtensionsTargetDescriptor} — Target directory + content type binding</li>
 *   <li>{@link AiAssistExtensionsTargetDirManifest} — Per-directory installed-file manifest</li>
 *   <li>{@link AiAssistExtensionsInstallationsDescriptor} — Fcli-state registry of installed assistants</li>
 * </ul>
 *
 * <h2>Output descriptors</h2>
 * <ul>
 *   <li>{@link AiAssistExtensionsOutputDescriptor} — Setup/uninstall/list-installed output</li>
 *   <li>{@link AiAssistExtensionsVersionOutputDescriptor} — List-versions output</li>
 *   <li>{@link AiAssistExtensionsAssistantOutputDescriptor} — List-assistants output</li>
 * </ul>
 */
package com.fortify.cli.ai_assist.extensions.helper;
