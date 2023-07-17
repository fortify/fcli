/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc._common.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import picocli.CommandLine.Command;

/**
 * <p>This class provides SSC-specific {@link IOutputHelper} implementations.</p>
 * 
 * @author rsenden
 */
public class SSCOutputHelperMixins {
    public static class AppVersionRefreshMettrics extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "refresh-metrics";
    }
    
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
