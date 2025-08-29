package com.fortify.cli.aviator.applyRemediation;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor;
import com.fortify.cli.aviator.util.FPRLoadingUtil;
import com.fortify.cli.aviator.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ApplyAutoRemediationOnSource {
    private static final Logger LOG = LoggerFactory.getLogger(ApplyAutoRemediationOnSource.class);

    public static int[] applyRemediations(File file, String sourceCodeDirectory, IAviatorLogger logger)
            throws AviatorSimpleException, AviatorTechnicalException{
            LOG.info("Starting apply auto-remediation process for file: {}", file.getPath());
        Path extractedPath;
        try {
            extractedPath = ZipUtils.extractZip(file.getPath());
            LOG.debug("Extracted FPR to path: {}", extractedPath);
        } catch (IOException e) {
            LOG.error("Failed to extract FPR file: {}", file.getPath(), e);
            throw new AviatorTechnicalException("Unable to extract FPR file due to an I/O error.", e);
        }

        try {
            if (!FPRLoadingUtil.hasRemediations(file)) {
                LOG.error("FPR file does not contain remediations.xml file: {}", file);
                throw new AviatorSimpleException("FPR file does not contain remediations.xml file.");
            }
        } catch (IOException e) {
            LOG.error("I/O error checking FPR remediations presence: {}", file.getPath(), e);
            throw new AviatorTechnicalException("I/O error checking FPR remediations presence.", e);
        }
        LOG.info("FPR validation successful");

        RemediationProcessor remediationProcessor = new RemediationProcessor(extractedPath, sourceCodeDirectory);
        return remediationProcessor.processRemediationXML();

    }
}
