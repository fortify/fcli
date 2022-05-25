package com.fortify.cli.tools.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.tools.model.ToolPackages;
import io.micronaut.core.annotation.ReflectiveAccess;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@ReflectiveAccess
public class ToolPackagesYamlHelper {
    public ToolPackages toolPackages;

    public ToolPackagesYamlHelper(){
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            URL url_toolPackages = classLoader.getResource("ToolPackages.yaml");
            System.out.println("TOOLPACKAGES PATH:" + url_toolPackages);
            InputStream file = classLoader.getResourceAsStream("ToolPackages.yaml");      // Works in Java
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            toolPackages = om.readValue(file, ToolPackages.class);
        } catch (IOException e) {
            System.out.println("An error occurred when trying to read ToolPackages.yaml. Will try one more time.");
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
