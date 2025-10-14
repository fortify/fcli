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
package com.fortify.cli.aviator.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;

public class ResourceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtil.class);
    private static <T> T loadYamlInternal(InputStream inputStream, Class<T> configClass) throws IOException, YAMLException {
        if (inputStream == null) {
            LOG.warn("WARN: InputStream is null, cannot load YAML for class {}", configClass.getSimpleName());
            return null;
        }
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setAllowRecursiveKeys(true);
        Constructor constructor = new Constructor(configClass, options);
        Yaml yaml = new Yaml(constructor);
        T loadedConfig = yaml.load(inputStream);
        if (loadedConfig == null) {
            LOG.warn("Loaded YAML for {} but the result is null (possibly empty or only comments).", configClass.getSimpleName());
        }
        return loadedConfig;
    }

    public static <T> T loadYamlResource(String resourceName, Class<T> configClass) {
        LOG.debug("Loading YAML resource: {} for class {}", resourceName, configClass.getSimpleName());
        ClassLoader classLoader = ResourceUtil.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                LOG.error("Classpath resource not found: {}", resourceName);
                throw new AviatorBugException("Internal fcli error: Classpath resource not found: " + resourceName);
            }
            T config = loadYamlInternal(inputStream, configClass);
            if (config == null) {
                LOG.error("Failed to load or parse YAML from classpath resource: {}. The resource might be empty or malformed.", resourceName);
                throw new AviatorBugException("Internal fcli error: Failed to load/parse classpath resource: " + resourceName);
            }
            return config;
        } catch (IOException e) {
            LOG.error("IO error loading YAML from classpath resource: {}", resourceName, e);
            throw new AviatorBugException("Internal fcli error: IO error loading classpath resource: " + resourceName, e);
        } catch (YAMLException e) {
            LOG.error("YAML parsing error for classpath resource: {}", resourceName, e);
            throw new AviatorBugException("Internal fcli error: YAML parsing error for classpath resource: " + resourceName, e);
        }
    }

    public static <T> T loadYamlFile(java.io.File configFile, Class<T> configClass) {
        LOG.debug("Loading YAML file: {} for class {}", configFile.getAbsolutePath(), configClass.getSimpleName());
        if (!configFile.exists()) {
            LOG.error("Configuration file not found: {}", configFile.getAbsolutePath());
            throw new AviatorTechnicalException("Configuration file not found: " + configFile.getAbsolutePath());
        }
        if (!configFile.isFile() || !configFile.canRead()) {
            LOG.error("Configuration file is not a regular file or cannot be read: {}", configFile.getAbsolutePath());
            throw new AviatorTechnicalException("Cannot access configuration file (not a file or unreadable): " + configFile.getAbsolutePath());
        }

        try (InputStream inputStream = new FileInputStream(configFile)) {
            T config = loadYamlInternal(inputStream, configClass);
            if (config == null) {
                LOG.warn("Failed to load or parse YAML from file: {}. The file might be empty or malformed.", configFile.getAbsolutePath());
                throw new AviatorSimpleException("Configuration file '" + configFile.getName() + "' is empty or has an invalid YAML format.");
            }
            return config;
        } catch (FileNotFoundException e) {
            LOG.error("Configuration file not found (should have been caught earlier): {}", configFile.getAbsolutePath(), e);
            throw new AviatorTechnicalException("Configuration file not found: " + configFile.getAbsolutePath(), e);
        } catch (IOException e) {
            LOG.error("IO error loading YAML from file: {}", configFile.getAbsolutePath(), e);
            throw new AviatorTechnicalException("Error reading configuration file '" + configFile.getName() + "'.", e);
        } catch (YAMLException e) {
            LOG.error("YAML parsing error for file: {}", configFile.getAbsolutePath(), e);
            throw new AviatorSimpleException("Invalid YAML format in configuration file '" + configFile.getName() + "': " + e.getMessage(), e);
        }
    }
}