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
package com.fortify.cli.aviator._common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator.config.ExtensionsConfig;
import com.fortify.cli.aviator.config.LanguagesCommentConfig;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.LanguageCommentMapperUtil;
import com.fortify.cli.aviator.util.ResourceUtil;

public class AviatorConfigManager {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorConfigManager.class);
    private static final String EXTENSIONS_CONFIG_RESOURCE = "extensions_config.yaml";
    private static final String LANGUAGES_COMMENT_CONFIG_RESOURCE = "languages_comment_config.yaml";
    private static final String DEFAULT_TAG_MAPPING_RESOURCE = "default_tag_mapping.yaml";
    private static final String DEFAULT_DAST_TAG_MAPPING_RESOURCE = "default_dast_tag_mapping.yaml";

    private static volatile AviatorConfigManager instance;
    private static final Object lock = new Object();

    private final ExtensionsConfig extensionsConfig;
    private final LanguagesCommentConfig languagesCommentConfig;
    private final TagMappingConfig defaultTagMappingConfig;
    private final TagMappingConfig defaultDastTagMappingConfig;

    private AviatorConfigManager() {
        LOG.debug("Initializing AviatorConfigManager...");
        this.extensionsConfig = ResourceUtil.loadYamlResource(EXTENSIONS_CONFIG_RESOURCE, ExtensionsConfig.class);
        this.languagesCommentConfig = ResourceUtil.loadYamlResource(LANGUAGES_COMMENT_CONFIG_RESOURCE, LanguagesCommentConfig.class);
        this.defaultTagMappingConfig = ResourceUtil.loadYamlResource(DEFAULT_TAG_MAPPING_RESOURCE, TagMappingConfig.class);
        this.defaultDastTagMappingConfig = ResourceUtil.loadYamlResource(DEFAULT_DAST_TAG_MAPPING_RESOURCE, TagMappingConfig.class);

        if (this.extensionsConfig != null) {
            FileTypeLanguageMapperUtil.initializeConfig(this.extensionsConfig);
            LOG.debug("FileTypeLanguageMapperUtil initialized.");
        } else {
            LOG.error("ExtensionsConfig is null, FileTypeLanguageMapperUtil cannot be initialized properly.");
        }
        if (this.languagesCommentConfig != null) {
            LanguageCommentMapperUtil.initializeConfig(this.languagesCommentConfig);
            LOG.debug("LanguageCommentMapperUtil initialized.");
        } else {
            LOG.error("LanguagesCommentConfig is null, LanguageCommentMapperUtil cannot be initialized properly.");
        }
        LOG.debug("AviatorConfigManager initialized successfully.");
    }

    public static AviatorConfigManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AviatorConfigManager();
                }
            }
        }
        return instance;
    }

    public ExtensionsConfig getExtensionsConfig() {
        if (extensionsConfig == null) {
            LOG.error("ExtensionsConfig was not loaded. This indicates a bug.");
            throw new AviatorBugException("Critical: ExtensionsConfig not loaded.");
        }
        return extensionsConfig;
    }

    public LanguagesCommentConfig getLanguagesCommentConfig() {
        if (languagesCommentConfig == null) {
            LOG.error("LanguagesCommentConfig was not loaded. This indicates a bug.");
            throw new AviatorBugException("Critical: LanguagesCommentConfig not loaded.");
        }
        return languagesCommentConfig;
    }

    public TagMappingConfig getDefaultTagMappingConfig() {
        if (defaultTagMappingConfig == null) {
            LOG.error("DefaultTagMappingConfig was not loaded. This indicates a bug.");
            throw new AviatorBugException("Critical: DefaultTagMappingConfig not loaded.");
        }
        return defaultTagMappingConfig;
    }

    public TagMappingConfig getDefaultDastTagMappingConfig() {
        if (defaultDastTagMappingConfig == null) {
            LOG.error("DefaultDastTagMappingConfig was not loaded. This indicates a bug.");
            throw new AviatorBugException("Critical: DefaultDastTagMappingConfig not loaded.");
        }
        return defaultDastTagMappingConfig;
    }
}