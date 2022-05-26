package com.fortify.cli.tools.model;

import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.ArrayList;

@ReflectiveAccess
public class ToolPackages {
    public ArrayList<ToolPackage> Packages;

    public ToolPackages(ArrayList<ToolPackage> Packages){
        this.Packages = Packages;
    }

    public ToolPackages(){}
}
