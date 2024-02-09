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
package com.fortify.cli.fod._common.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import picocli.CommandLine.Command;

/**
 * <p>This class provides FoD-specific {@link IOutputHelper} implementations.</p>
 *
 * @author rsenden
 */
public class FoDOutputHelperMixins {
    public static class DownloadLatest extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "download-latest";
    }
    public static class SetupSast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-sast";
    }
    public static class SetupDast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-dast";
    }
    public static class SetupMobile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-mobile";
    }
    public static class SetupWebsite extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-website";
    }
    public static class SetupWorkflow extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-workflow";
    }
    public static class SetupApi extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-api";
    }
    public static class GetConfig extends OutputHelperMixins.DetailsNoQuery {
        public static final String CMD_NAME = "get-config";
    }
    public static class GetSast extends OutputHelperMixins.DetailsNoQuery {
        public static final String CMD_NAME = "get-sast";
    }
    public static class GetDast extends OutputHelperMixins.DetailsNoQuery {
        public static final String CMD_NAME = "get-dast";
    }
    public static class GetMobile extends OutputHelperMixins.DetailsNoQuery {
        public static final String CMD_NAME = "get-mobile";
    }

    public static class StartSast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-sast";
    }
    public static class StartDast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-dast";
    }
    public static class StartMobile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-mobile";
    }

    public static class ImportScan extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-scan";
    }

    public static class ImportSast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-sast";
    }
    public static class ImportDast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-dast";
    }
    public static class ImportMobile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-mobile";
    }
    @Command(aliases = "import-oss")
    public static class ImportOpenSource extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-open-source";
    }

    public static class Lookup extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "lookup";
    }

    public static class DownloadResults extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "download-results";
    }

    public static class AssessmentType extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "assessment-type";
    }

    public static class StartLegacy extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-legacy";
    }
    public static class GetConfigLegacy extends OutputHelperMixins.DetailsNoQuery {
        public static final String CMD_NAME = "get-config-legacy";
    }

    public static class UploadFile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "upload-file";
    }
}
