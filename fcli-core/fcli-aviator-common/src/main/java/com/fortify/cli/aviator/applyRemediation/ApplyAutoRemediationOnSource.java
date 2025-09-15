package com.fortify.cli.aviator.applyRemediation;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ApplyAutoRemediationOnSource {
    private static final Logger LOG = LoggerFactory.getLogger(ApplyAutoRemediationOnSource.class);

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory, IAviatorLogger logger)
            throws AviatorSimpleException, AviatorTechnicalException {

        LOG.info("Starting apply auto-remediation process for file: {}", fprHandle.getFprPath());

        if (!fprHandle.hasRemediations()) {
            LOG.error("FPR file does not contain remediations.xml file: {}", fprHandle.getFprPath());
            throw new AviatorSimpleException("FPR file does not contain remediations.xml file.");
        }
        LOG.info("FPR validation successful");

        RemediationProcessor remediationProcessor = new RemediationProcessor(fprHandle, sourceCodeDirectory);
        return remediationProcessor.processRemediationXML();

    }
}
