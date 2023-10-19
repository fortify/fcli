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
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins.DetailsNoQuery;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins.TableNoQuery;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins.TableWithQuery;

import picocli.CommandLine.Command;

/**
 * <p>This class provides SSC-specific {@link IOutputHelper} implementations.</p>
 * 
 * @author rsenden
 */
public class SSCOutputHelperMixins {
    public static class AppVersionDownloadState extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "download-state"; 
    }
     
    public static class AppVersionPurgeArtifacts extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "purge-artifacts";
    }
    
    public static class AppVersionRefreshMettrics extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "refresh-metrics";
    }
    
    public static class ArtifactApprove extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "approve";
    }
    
    public static class ArtifactPurge extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "purge";
    }
    
    public static class ImportDebricked extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-debricked";
    }
    
    @Command(aliases = {"lsfs"})
    public static class ListFilterSets extends TableWithQuery {
        public static final String CMD_NAME = "list-filtersets";
    }

    public static class GetFilterSet extends DetailsNoQuery {
        public static final String CMD_NAME = "get-filterset";
    }
    
    @Command(aliases = {"lsf"})
    public static class ListFilters extends TableWithQuery {
        public static final String CMD_NAME = "list-filters";
    }
    
    public static class GetFilter extends DetailsNoQuery {
        public static final String CMD_NAME = "get-filter";
    }
    
    @Command(aliases = {"lsg"})
    public static class ListGroups extends TableWithQuery {
        public static final String CMD_NAME = "list-groups";
    }

    public static class GetGroup extends DetailsNoQuery {
        public static final String CMD_NAME = "get-group";
    }
    
    @Command(aliases = {"lsp"})
    public static class ListPermissions extends TableWithQuery {
        public static final String CMD_NAME = "list-permissions";
    }

    public static class GetPermission extends DetailsNoQuery {
        public static final String CMD_NAME = "get-permission";
    }
    
    public static class VulnCount extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "count";
    }
     
    @Command(aliases = {"lsa"})
    public static class ListActivities extends TableWithQuery {
        public static final String CMD_NAME = "list-activities";
    } 
    
    @Command(aliases = {"lse"})
    public static class ListEvents extends TableWithQuery {
        public static final String CMD_NAME = "list-events";
    } 
    
    @Command(aliases = {"lsj"})
    public static class ListJobs extends TableWithQuery {
        public static final String CMD_NAME = "list-jobs";
    } 
    
    public static class CancelJob extends TableNoQuery {
        public static final String CMD_NAME = "cancel-job";
    }
    
    public static class UpdateJob extends TableNoQuery {
        public static final String CMD_NAME = "update-job";
    }
    
    public static class GetJob extends DetailsNoQuery {
        public static final String CMD_NAME = "get-job";
    }
    
    public static class UploadSeedBundle extends TableNoQuery {
        public static final String CMD_NAME = "upload-seed-bundle";
    }
}
