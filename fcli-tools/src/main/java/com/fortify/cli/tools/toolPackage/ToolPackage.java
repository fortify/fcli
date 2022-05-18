package com.fortify.cli.tools.toolPackage;

import com.fortify.cli.tools.picocli.command.mixin.DownloadPathMixin;
import com.fortify.cli.tools.picocli.command.mixin.DownloadUrlMixin;
import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

public abstract class ToolPackage {

    @Getter private String DownloadURL;
    @Getter private String TempDir;
    @Getter private String InstallPath;
    @Getter private String DownloadPath;
    @Getter private Map<String, String> EnvVariablesToCreate;
    @Getter private ArrayList<String> EnvVariablesToRead;
    @Getter private String UnderConstructionMsg = "Work in progress. Come back later.";

    public ToolPackage(String DownloadURL, String TempDir, String InstallPath, String DownloadPath,
                       Map<String, String> EnvVariablesToCreate, ArrayList<String> EnvVariablesToRead){
        this.DownloadURL = DownloadURL;
        this.TempDir = TempDir;
        this.InstallPath = InstallPath;
        this.DownloadPath = DownloadPath;
        this.EnvVariablesToCreate = EnvVariablesToCreate;
        this.EnvVariablesToRead = EnvVariablesToRead;
    }

    @Command(name = "download", description = "Only download to a specified directory.")
    public void Download(@Mixin DownloadPathMixin opt){
        if(DownloadPath == null){
            System.out.println(getUnderConstructionMsg());
            if(!opt.DownloadPath.isBlank())
                System.out.println("But in the future, I'll will totally download to: " + opt.DownloadPath);
        }
    }

    public abstract void Install(InstallPathMixin opt);

    public abstract void Uninstall();
}
