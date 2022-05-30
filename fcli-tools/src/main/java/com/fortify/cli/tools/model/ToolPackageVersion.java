package com.fortify.cli.tools.model;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;

@ReflectiveAccess
public class ToolPackageVersion{
    @Getter @Setter private String Version;
    @Getter @Setter private String Url;
    @Setter private String Checksum;

    public ToolPackageVersion(String Version, String Url){
        this.Url = Url;
        this.Version = Version;
    }

    public ToolPackageVersion(){}

    public String getFileName(){
        String[] parts = this.getUrl().split("/");
        return parts[parts.length-1];
    }

    public Checksum getChecksum(){
        if(this.Checksum == null)
            return null;
        return new Checksum(this.Checksum);
    }

}