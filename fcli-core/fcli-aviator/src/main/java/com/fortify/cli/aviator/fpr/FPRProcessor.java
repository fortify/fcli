package com.fortify.cli.aviator.fpr;

import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.filter.FilterTemplateParser;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;



public class FPRProcessor {

    Logger logger = LoggerFactory.getLogger(FPRProcessor.class);
    private final Path FPRPath;
    private final Path extractedPath;
    private final Map<String, AuditIssue> auditIssueMap;
    @Getter
    private FPRInfo fprInfo;
    private final AuditProcessor auditProcessor;

    public FPRProcessor(String fprPath, Path extractedPath, Map<String, AuditIssue> auditIssueMap, AuditProcessor auditProcessor) {
        this.FPRPath = Paths.get(fprPath);
        this.extractedPath = extractedPath;
        this.auditIssueMap = auditIssueMap;
        this.auditProcessor = auditProcessor;
    }

    public List<Vulnerability> process() throws Exception {

        logger.info("FPR Processing started");

        fprInfo = new FPRInfo(extractedPath);

        FilterTemplateParser filterTemplateParser = new FilterTemplateParser(extractedPath, auditProcessor);
        Optional<FilterTemplate> filterTemplate = filterTemplateParser.parseFilterTemplate();
        filterTemplate.ifPresent(ft -> fprInfo.setFilterTemplate(ft));

        if (filterTemplate.isPresent()) {
            filterTemplate.flatMap(f -> f.getFilterSets().stream().filter(FilterSet::isEnabled).findFirst()).ifPresent(fprInfo::setDefaultEnabledFilterSet);
        }

        logger.debug("Audit.xml Issues: {}", auditIssueMap.keySet().size());

        FVDLProcessor fvdlProcessor = new FVDLProcessor(extractedPath);
        fvdlProcessor.processXML();

        List<Vulnerability> vulnerabilities = fvdlProcessor.getVulnerabilities();
        logger.debug("Number of Issues: {}", vulnerabilities.size());

        return vulnerabilities;
    }
}