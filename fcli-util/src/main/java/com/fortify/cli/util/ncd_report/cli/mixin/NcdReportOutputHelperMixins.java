package com.fortify.cli.util.ncd_report.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

/**
 * <p>This class provides {@link IOutputHelper} implementations for NCD reports.</p>
 * 
 * @author rsenden
 */
public class NcdReportOutputHelperMixins {
    public static class NcdReportGenerate extends OutputHelperMixins.DetailsNoQuery {
        public static final String CMD_NAME = "generate";
    }
}
