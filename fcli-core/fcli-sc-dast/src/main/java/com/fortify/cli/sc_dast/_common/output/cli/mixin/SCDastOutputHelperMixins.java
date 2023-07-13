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
package com.fortify.cli.sc_dast._common.output.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

/**
 * <p>This class provides SC-DAST-specific {@link IOutputHelper} implementations.</p>
 * 
 * @author rsenden
 */
public class SCDastOutputHelperMixins {
     public static class ScanActionComplete extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "complete";
    }
    
     public static class ScanActionImportFindings extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-findings";
    }
    
     public static class ScanActionPause extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "pause";
    }
    
     public static class ScanActionPublish extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "publish";
    }
    
     public static class ScanActionResume extends OutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "resume";
    }
}
