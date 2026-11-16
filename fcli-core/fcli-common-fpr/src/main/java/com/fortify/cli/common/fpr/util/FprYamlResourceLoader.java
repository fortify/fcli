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

import java.io.IOException;
import java.io.InputStream;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.fortify.cli.common.exception.FcliBugException;

/** Loads the YAML configuration files bundled with the FPR parsing code from the classpath. */
final class FprYamlResourceLoader {
    private FprYamlResourceLoader() {}

    static <T> T load(String resourceName, Class<T> configClass) {
        try (InputStream inputStream = FprYamlResourceLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new FcliBugException("Internal fcli error: Classpath resource not found: " + resourceName);
            }
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setAllowRecursiveKeys(true);
            T config = new Yaml(new Constructor(configClass, options)).load(inputStream);
            if (config == null) {
                throw new FcliBugException("Internal fcli error: Failed to load/parse classpath resource: " + resourceName);
            }
            return config;
        } catch (IOException | YAMLException e) {
            throw new FcliBugException("Internal fcli error: Failed to load classpath resource: " + resourceName, e);
        }
    }
}
