package com.fortify.cli.tools.model;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;

@ReflectiveAccess
public class ToolPackageVersion{
    @Getter @Setter private String Version;
    @Getter @Setter private String Url;

    public ToolPackageVersion(String Version, String Url){
        this.Url = Url;
        this.Version = Version;
    }

    public ToolPackageVersion(){}

    public String GetFileName(){
        String[] parts = this.getUrl().split("/");
        return parts[parts.length-1];
    }
}