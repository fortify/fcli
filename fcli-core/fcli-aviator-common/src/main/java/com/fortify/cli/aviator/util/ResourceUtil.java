package com.fortify.cli.aviator.util;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ResourceUtil {
    private static final Logger logger = LoggerFactory.getLogger(ResourceUtil.class);

    public static <T> T loadYamlConfig(String resourceName, Class<T> configClass) throws IOException {
        ClassLoader classLoader = ResourceUtil.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setAllowRecursiveKeys(true);
            Constructor constructor = new Constructor(configClass, options);
            Yaml yaml = new Yaml(constructor);
            return yaml.load(inputStream);
        } catch (IOException e) {
            logger.error("Error loading YAML config from resource: " + resourceName, e);
            throw e;
        }
    }
}