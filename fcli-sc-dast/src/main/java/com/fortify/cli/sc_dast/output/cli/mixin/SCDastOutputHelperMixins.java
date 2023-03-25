package com.fortify.cli.sc_dast.output.cli.mixin;

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
