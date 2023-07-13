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
package com.fortify.cli.fod._common.output.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

/**
 * <p>This class provides FoD-specific {@link IOutputHelper} implementations.</p>
 *
 * @author rsenden
 */
public class FoDOutputHelperMixins {

    public static class CreateWebApp extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "create-web-app";
    }
    public static class CreateMicroserviceApp extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "create-microservice-app";
    }
    public static class CreateMobileApp extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "create-mobile-app";
    }

     public static class ListSast extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-sast";
    }
     public static class ListDast extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-dast";
    }
     public static class ListOss extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-oss";
    }
     public static class ListMobile extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-mobile";
    }

     public static class SetupSast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-sast";
    }
     public static class SetupDast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-dast";
    }
     public static class SetupOss extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-oss";
    }
     public static class SetupMobile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-mobile";
    }

     public static class StartSast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-sast";
    }
     public static class StartDast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-dast";
    }
     public static class StartOss extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-oss";
    }
     public static class StartMobile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-mobile";
    }

     public static class ImportSast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-sast";
    }
     public static class ImportDast extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-dast";
    }
     public static class ImportOss extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-oss";
    }
     public static class ImportMobile extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-mobile";
    }

}
