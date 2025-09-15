package com.fortify.cli.aviator.fpr;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FVDLProcessor;
import com.fortify.cli.aviator.fpr.processor.FilterTemplateParser;
import com.fortify.cli.aviator.util.FprHandle;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FPRProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FPRProcessor.class);
    private final FprHandle fprHandle;
    private final Map<String, AuditIssue> auditIssueMap;
    private final AuditProcessor auditProcessor;

    @Getter
    private FPRInfo fprInfo;

    public FPRProcessor(FprHandle fprHandle, Map<String, AuditIssue> auditIssueMap, AuditProcessor auditProcessor) {
        this.fprHandle = fprHandle;
        this.auditIssueMap = auditIssueMap;
        this.auditProcessor = auditProcessor;
    }

    /**
     * Processes the main components of the FPR.
     *
     * @param fvdlProcessor The processor for handling the audit.fvdl file.
     * @return A list of all vulnerabilities found in the FVDL.
     */
    public List<Vulnerability> process(FVDLProcessor fvdlProcessor) {
        logger.info("FPR Processing started");
      try{
          this.fprInfo = new FPRInfo(this.fprHandle);

          FilterTemplateParser filterTemplateParser = new FilterTemplateParser(this.fprHandle, auditProcessor);
          Optional<FilterTemplate> filterTemplateOpt = filterTemplateParser.parseFilterTemplate();


        if (filterTemplateOpt.isPresent()) {
            FilterTemplate filterTemplate = filterTemplateOpt.get();
            this.fprInfo.setFilterTemplate(filterTemplate);

            filterTemplate.getFilterSets().stream()
                    .filter(FilterSet::isEnabled)
                    .findFirst()
                    .ifPresent(this.fprInfo::setDefaultEnabledFilterSet);

            logger.debug("Filter template loaded successfully.");
            if (this.fprInfo.getDefaultEnabledFilterSet().isPresent()) {
                logger.info("Found default enabled filter set: '{}'", this.fprInfo.getDefaultEnabledFilterSet().get().getTitle());
            } else {
                logger.info("No default enabled filter set found in template.");
            }
        } else {
            logger.warn("No filter template found. Proceeding without any available filter sets.");
        }

        logger.debug("Audit.xml Issues found: {}", auditIssueMap.size());

        List<Vulnerability> vulnerabilities = fvdlProcessor.processXML();
        logger.info("Parsed {} vulnerabilities from FVDL.", vulnerabilities.size());

        return vulnerabilities;
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
          logger.error("Unexpected error during FPR processing", e);
          throw new AviatorTechnicalException("Unexpected error during FPR processing.", e);
        }
    }
}