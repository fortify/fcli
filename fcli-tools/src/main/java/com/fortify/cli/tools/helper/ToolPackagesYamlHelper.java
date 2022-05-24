package com.fortify.cli.tools.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.tools.model.ToolPackages;
import java.io.IOException;
import java.io.InputStream;

public class ToolPackagesYamlHelper {
    public ToolPackages toolPackages;

    public ToolPackagesYamlHelper(){
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream file = classLoader.getResourceAsStream("ToolPackages.yaml");

        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        try {
            toolPackages = om.readValue(file, ToolPackages.class);
        } catch (IOException e) {
            System.out.println("An error occurred when trying to read ToolPackages.yaml.");
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
