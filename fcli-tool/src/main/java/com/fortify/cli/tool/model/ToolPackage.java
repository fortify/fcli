package com.fortify.cli.tool.model;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@ReflectiveAccess
public class ToolPackage{
    @Getter @Setter private String Name;
    @Getter @Setter private String DefaultVersion;
    @Getter @Setter private ArrayList<ToolPackageVersion> Versions;

    public ToolPackage(String Name, String DefaultVersion, ArrayList<ToolPackageVersion> Versions){
        this.Name = Name;
        this.DefaultVersion = DefaultVersion;
        this.Versions = Versions;
    }

    public ToolPackage(){}
}