package com.fortify.cli.ssc.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import picocli.CommandLine.Command;

/**
 * <p>This class provides SSC-specific {@link IOutputHelper} implementations.</p>
 * 
 * @author rsenden
 */
public class SSCOutputHelperMixins {
    public static class ArtifactApprove extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "approve";
    }
    
    @Command(aliases = "download-by-id")
     public static class ArtifactDownloadById 
                extends OutputHelperMixins.Download {}
    
     public static class ArtifactDownloadState extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "download-state"; 
    }
    
    @Command(aliases = "purge-by-id")
     public static class ArtifactPurgeById extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "purge";
    }
    
     public static class ArtifactPurgeOlderThan extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "purge-older-than";
    }
    
     public static class ImportDebricked extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-debricked";
    }
    
     public static class VulnCount extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "count";
    }
}
