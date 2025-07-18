package com.fortify.cli.aviator.fpr;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.model.Vulnerability;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FVDLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.processor.FilterTemplateParser;

import lombok.Getter;

public class FPRProcessor {

    Logger LOG = LoggerFactory.getLogger(FPRProcessor.class);
    private final Path extractedPath;
    private final Map<String, AuditIssue> auditIssueMap;
    @Getter
    private FPRInfo fprInfo;
    private final AuditProcessor auditProcessor;

    public FPRProcessor(String fprPath, Path extractedPath, Map<String, AuditIssue> auditIssueMap, AuditProcessor auditProcessor, FVDLProcessor fvdlProcessor) {
        this.extractedPath = extractedPath;
        this.auditIssueMap = auditIssueMap;
        this.auditProcessor = auditProcessor;
    }

    public List<Vulnerability> process(FVDLProcessor fvdlProcessor) throws AviatorTechnicalException {
        LOG.info("FPR Processing started");

        try {
            fprInfo = new FPRInfo(extractedPath);

            FilterTemplateParser filterTemplateParser = new FilterTemplateParser(extractedPath, auditProcessor);
            Optional<FilterTemplate> filterTemplate = filterTemplateParser.parseFilterTemplate();
            filterTemplate.ifPresent(ft -> fprInfo.setFilterTemplate(ft));

            if (filterTemplate.isPresent()) {
                filterTemplate.flatMap(f -> f.getFilterSets().stream().filter(FilterSet::isEnabled).findFirst()).ifPresent(fprInfo::setDefaultEnabledFilterSet);
            }

            LOG.debug("Audit.xml Issues: {}", auditIssueMap.keySet().size());

            fvdlProcessor.processXML();

            List<Vulnerability> vulnerabilities = fvdlProcessor.getVulnerabilities();
            LOG.debug("Number of Issues: {}", vulnerabilities.size());

            return vulnerabilities;
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error during FPR processing", e);
            throw new AviatorTechnicalException("Unexpected error during FPR processing.", e);
        }
    }
}