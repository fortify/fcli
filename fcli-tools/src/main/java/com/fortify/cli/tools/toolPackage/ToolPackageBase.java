package com.fortify.cli.tools.toolPackage;

import com.fortify.cli.common.http.HttpDownloadHelper;
import com.fortify.cli.tools.helper.ToolPackagesYamlHelper;
import com.fortify.cli.tools.model.ToolPackage;
import com.fortify.cli.tools.model.ToolPackageVersion;
import com.fortify.cli.tools.picocli.command.mixin.DownloadPathMixin;
import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tools.picocli.command.mixin.PackageVersionMixin;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * #ToolPackageBase serves as a base class for "tool-package" command classes, which are classes responsible for
 * downloading and/or installing (in addition to other functions) tools or packages that an end-user may find useful.
 * "tools and packages" can be things like sample source code, plugins, and even CLI tools.
 *
 * In order for the #ToolPackageBase class to function correctly, a corresponding entry in the 'ToolPackages.yaml" file
 * needs to be created. The naming convention of #ToolPackageBase subclasses and "ToolPackages.yaml" name entries are
 * the following.
 *   - For "ToolPackages.yaml" entries: <ToolOrPackageName>
 *   - For #ToolPackageBase subclasses: <ToolOrPackageName>+"Commands"
 */
@ReflectiveAccess
public abstract class ToolPackageBase {

    @Getter private final String UnderConstructionMsg = "Work in progress. Come back later.";
    private final ToolPackage toolPackage;

    public ToolPackageBase(){
        this.toolPackage = getToolPackage(getToolPackagName());
    }

    @Command(name = "download", description = "Only download to a specified directory.")
    public void Download(@Mixin DownloadPathMixin dpo, @Mixin PackageVersionMixin pv){
        String downloadPath = dpo.DownloadPath == null ? "./" + getToolPackagName() : dpo.DownloadPath ;
        DownloadVersion(downloadPath, pv.DownloadPackageVersion);
    }

    /**
     * When subclassed, custom logic for how to install the tool-package will be implemented here.
     * @param installPath       The directory where the tool should be installed.
     * @param packageVersion    The version of the tool or package to be retrieved and installed.
     */
    public abstract void Install(InstallPathMixin installPath,  PackageVersionMixin packageVersion);

    /**
     * When subclassed, custom logic for how to uninstall the tool-package will be implemented here.
     * @param pv The version of the package you wish to uninstall.
     */
    public abstract void Uninstall(PackageVersionMixin pv);

    /**
     * Lists all versions available for download
     * @param printPackageVersionUrls For all versions listed, the associated download URL will be printed. By default, these URLs are not printed.
     */
    @Command(
            name = "list",
            description = "List which versions can be downloaded and/or installed."
    )
    public void ListVersions(
        @CommandLine.Option(names = {"-p", "--printUrl"}, description = "Print the URLs used to download packages from.")
        boolean printPackageVersionUrls
    ){
        String pkgName = getToolPackagName();

        if(toolPackage == null || toolPackage.getVersions() == null){
            System.out.println("Unable to find versions for package: " + pkgName);
            System.exit(-1);
        }

        System.out.println("VERSIONS (" + pkgName + "): ");
        for (ToolPackageVersion tpv : toolPackage.getVersions()) {
            if(printPackageVersionUrls){
                String versionInfo = String.format("  - %s (%s)", tpv.getVersion(), tpv.getUrl());
                System.out.println(versionInfo);
            }else {
                System.out.println("  - " + tpv.getVersion());
            }
        }
    }

    /**
     *
     * @param toolPkgName   The name of the ToolPackage to get.
     * @return              Object instance of the ToolPackage being worked on.
     */
    private ToolPackage getToolPackage(String toolPkgName) {
        for (ToolPackage tp : new ToolPackagesYamlHelper().toolPackages.Packages) {
            if (toolPkgName.equalsIgnoreCase(tp.getName())) {
                return tp;
            }
        }
        return null;
    }

    /**
     * Returns the name of the "tool package" that should be interacted with as a string.
     * The "tool package" name is retrieved by getting the class name of the calling subclass. If the subclass's
     * class name is suffixed with the word "Commands" then that will be stripped before being returned.
     * @return  The name of the ToolPackage
     */
    private String getToolPackagName() {
        String pkgName = this.getClass().getSimpleName();
        int suffixIndex = this.getClass().getSimpleName().indexOf("Commands");
        if(suffixIndex > 0)
            pkgName = pkgName.substring(0,suffixIndex);
        return pkgName;
    }

    /* Basic logic here is:
     *   - If version selected is "manual", then exit.
     *   - If no download path is specified then current directory, with resource name from the URL, will be used.
     *   - If no version is specified, then the default version will be downloaded.
     */
    private void DownloadVersion(String downloadPath, String versionToDownload) {
        String manual = "manual";
        versionToDownload = versionToDownload == null ? toolPackage.getDefaultVersion() : versionToDownload;

        if(manual.equals(versionToDownload.toLowerCase())){
            for (ToolPackageVersion version : toolPackage.getVersions()) {
                if(version.getVersion().equalsIgnoreCase(manual)){
                    System.out.printf("Please open a browser and navigate to the following URL (%s)%n", version.getUrl());
                    System.exit(0);
                }
            }
            System.out.println("No manual download options available.");
            System.exit(1);
        }

        System.out.printf("Downloading (%s : %s)%n", getToolPackagName(), versionToDownload);
        try {
            for (ToolPackageVersion ver : toolPackage.getVersions()) {
                if(ver.getVersion().equalsIgnoreCase(versionToDownload))
                    HttpDownloadHelper.Download(new URL(ver.getUrl()), ".\\"+ver.GetFileName());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
