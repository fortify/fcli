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
package com.fortify.cli.common.fpr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.fpr.config.ExtensionsConfig;

public class FileTypeLanguageMapperUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileTypeLanguageMapperUtil.class);
    private static final String EXTENSIONS_CONFIG_RESOURCE = "extensions_config.yaml";
    private static volatile ExtensionsConfig extensionsConfigInstance;
    private static final Object initLock = new Object();
    private static volatile boolean initialized = false;

    public static void initializeConfig(ExtensionsConfig loadedExtensionsConfig) {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    if (loadedExtensionsConfig == null) {
                        LOG.error("Attempted to initialize FileTypeLanguageMapperUtil with null ExtensionsConfig.");
                        throw new FcliBugException("FileTypeLanguageMapperUtil cannot be initialized: ExtensionsConfig is null.");
                    }
                    extensionsConfigInstance = loadedExtensionsConfig;
                    initialized = true;
                    LOG.debug("FileTypeLanguageMapperUtil has been initialized.");
                }
            }
        } else {
            LOG.trace("FileTypeLanguageMapperUtil already initialized, skipping re-initialization.");
        }
    }

    public static String getProgrammingLanguage(String fileExtension) {
        if (!initialized) {
            LOG.debug("FileTypeLanguageMapperUtil not explicitly initialized; loading bundled extensions config.");
            initializeConfig(FprYamlResourceLoader.load(EXTENSIONS_CONFIG_RESOURCE, ExtensionsConfig.class));
        }

        if (StringUtil.isEmpty(fileExtension)) {
            return "Unknown";
        }
        String ext = fileExtension.startsWith(".")
                ? fileExtension
                : "." + fileExtension;

        return extensionsConfigInstance != null
                ? extensionsConfigInstance.getLanguageForExtension(ext)
                : "Unknown";
    }
}