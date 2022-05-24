package com.fortify.cli.tools.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class ToolPackages {
    public ArrayList<ToolPackage> Packages;

    public ToolPackages(ArrayList<ToolPackage> Packages){
        this.Packages = Packages;
    }

    public ToolPackages(){}
}
