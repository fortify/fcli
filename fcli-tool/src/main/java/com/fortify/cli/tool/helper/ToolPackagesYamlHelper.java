package com.fortify.cli.tool.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.tool.model.ToolPackages;

import io.micronaut.core.annotation.ReflectiveAccess;
import java.io.IOException;
import java.io.InputStream;

@ReflectiveAccess
public class ToolPackagesYamlHelper {
    public ToolPackages toolPackages;

    public ToolPackagesYamlHelper(){
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            InputStream file = classLoader.getResourceAsStream("com/fortify/cli/tool/ToolPackages.yaml");
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            toolPackages = om.readValue(file, ToolPackages.class);
        } catch (IOException e) {
            System.out.println("An error occurred when trying to read ToolPackages.yaml.");
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
