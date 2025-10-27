/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.ssc.action.helper;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.fortify;

import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionSpelFunctions;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.SpelHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.ssc._common.rest.cli.mixin.SSCAndScanCentralUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper.SSCFileTransferTokenSupplier;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper.SSCFileTransferTokenType;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetHelper;

import kong.unirest.RawResponse;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor @Reflectable
@SpelFunctionPrefix("ssc.")
public final class SSCActionSpelFunctions {
    private final SSCAndScanCentralUnirestInstanceSupplierMixin unirestInstanceSupplier;
    private final ActionRunnerContext ctx;
    
    @SpelFunction(cat=fortify, returns="SSC application version object for the given application version name or id") 
    public final ObjectNode appVersion(
            @SpelFunctionParam(name="nameOrId", desc="the name or ID of the application version to load") String nameOrId) 
    {
        ctx.getProgressWriter().writeProgress("Loading application version %s", nameOrId);
        var result = SSCAppVersionHelper.getRequiredAppVersion(unirestInstanceSupplier.getSscUnirestInstance(),
                nameOrId, ":");
        ctx.getProgressWriter().writeProgress("Loaded application version %s", result.getAppAndVersionName());
        return result.asObjectNode();
    }

    @SpelFunction(cat=fortify, returns="SSC filter set object for the given application version and filter set title or id") 
    public final  JsonNode filterSet(
            @SpelFunctionParam(name="av", desc="an SSC application version object, for example as returned by `#ssc.appVersion(...)`, containing at least the `id` field") ObjectNode appVersion,
            @SpelFunctionParam(name="titleOrId", desc="the title or ID of the filter set to load; may be blank to load the default filter set") String titleOrId) 
    {
        var progressMessage = StringUtils.isBlank(titleOrId) ? "Loading default filter set"
                : String.format("Loading filter set %s", titleOrId);
        ctx.getProgressWriter().writeProgress(progressMessage);
        var filterSetDescriptor = new SSCIssueFilterSetHelper(unirestInstanceSupplier.getSscUnirestInstance(),
                appVersion.get("id").asText()).getDescriptorByTitleOrId(titleOrId, false);
        if (filterSetDescriptor == null) {
            throw new FcliSimpleException("Unknown filter set: " + titleOrId);
        }
        return filterSetDescriptor.asJsonNode();
    }

    @SpelFunction(cat=fortify, desc="""
            The return value of this function can be passed to a `records.for-each::from` instruction \
            to iterate over all rule descriptions that are referenced by issues in the given application \
            version. See built-in SSC sarif-sast-report.yaml action for sample usage. 
            """,
            returns="Processor for iterating over rule descriptions")
    public IActionStepForEachProcessor ruleDescriptionsProcessor(
            @SpelFunctionParam(name="avId", desc="the application version ID as a string") String appVersionId) 
    {
        var unirest = ctx.getRequestHelper("ssc").getUnirestInstance();
        return new SSCFPRRuleDescriptionProcessor(unirest, appVersionId)::process;
    }

    @SpelFunction(cat=fortify, returns="Browser-accessible URL pointing to the SSC issue details page for the given issue")
    public String issueBrowserUrl(
            @SpelFunctionParam(name="issue", desc="an SSC issue object, containing at least `projectVersionId`, `id`, `engineType`, and `issueInstanceId` fields") ObjectNode issue,
            @SpelFunctionParam(name="fs", desc="`null` to use default filter set, or an SSC filter set object, for example as returned by `#ssc.filterSet(...)`, containing at least the `guid` field") ObjectNode filterset) 
    {
        var deepLinkExpression = baseUrl()
                + "/html/ssc/version/${projectVersionId}/fix/${id}/?engineType=${engineType}&issue=${issueInstanceId}";
        if (filterset != null) {
            deepLinkExpression += "&filterSet=" + filterset.get("guid").asText();
        }
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue,
                String.class);
    }

    @SpelFunction(cat=fortify, returns="Browser-accessible URL pointing to the SSC application version page for the given application version")
    public String appversionBrowserUrl(
            @SpelFunctionParam(name="av", desc="an SSC application version object, for example as returned by `#ssc.appVersion(...)`, containing at least the `id` field") ObjectNode appVersion,
            @SpelFunctionParam(name="fs", desc="`null` to use default filter set, or an SSC filter set object, for example as returned by `#ssc.filterSet(...)`, containing at least the `guid` field") ObjectNode filterset)
    {
        var deepLinkExpression = baseUrl() + "/html/ssc/version/${id}/audit";
        if (filterset != null) {
            deepLinkExpression += "?filterSet=" + filterset.get("guid").asText();
        }
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appVersion,
                String.class);
    }

    private String baseUrl() {
        return unirestInstanceSupplier.getSessionDescriptor().getSscUrlConfig().getUrl()
                .replaceAll("/+$", "");
    }
    
    @RequiredArgsConstructor
    private static final class SSCFPRRuleDescriptionProcessor {
        private final UnirestInstance unirest;
        private final String appVersionId;
        
        @SneakyThrows
        public final void process(Function<JsonNode, Boolean> consumer) {
            try ( SSCFileTransferTokenSupplier tokenSupplier = new SSCFileTransferTokenSupplier(unirest, SSCFileTransferTokenType.DOWNLOAD); ) {
                unirest.get(SSCUrls.DOWNLOAD_CURRENT_FPR(appVersionId, false))
                    .routeParam("downloadToken", tokenSupplier.get()).asObject(r->processFpr(r, consumer)).getBody();
            }
        }
        
        @SneakyThrows
        private final String processFpr(RawResponse r, Function<JsonNode, Boolean> consumer) {
            try ( var zis = new ZipInputStream(r.getContent()) ) {
                ZipEntry entry;
                while ( (entry = zis.getNextEntry())!=null ) {
                    if ( "audit.fvdl".equals(entry.getName()) ) {
                        processAuditFvdl(zis, consumer); break;
                    }
                }
            }
            return null;
        }
        
        private final void processAuditFvdl(InputStream is, Function<JsonNode, Boolean> consumer) throws XMLStreamException {
            var factory = XMLInputFactory.newInstance();
            factory.setXMLResolver(null); // Prevent XML External Entity Injection
            var reader = factory.createXMLStreamReader(is);
            while(reader.hasNext()) {
                int eventType = reader.next();
                if ( eventType==XMLStreamReader.START_ELEMENT && "Description".equals(reader.getLocalName()) ) {
                    if (!processDescription(reader, consumer)) { break; }
                }
            }
        }

        private boolean processDescription(XMLStreamReader reader, Function<JsonNode, Boolean> consumer) throws XMLStreamException {
            var ruleId = reader.getAttributeValue(null, "classID");
            var entry = JsonHelper.getObjectMapper().createObjectNode()
                    .put("id", ruleId);
            var tips = JsonHelper.getObjectMapper().createArrayNode();
            var references = JsonHelper.getObjectMapper().createArrayNode();
            entry.set("tips", tips);
            entry.set("references", references);
            processElement(reader, name->{
                switch ( name ) {
                case "Abstract": entry.put("abstract", ActionSpelFunctions.cleanRuleDescription(readString(reader))); break;
                case "Explanation": entry.put("explanation", ActionSpelFunctions.cleanRuleDescription(readString(reader))); break;
                case "Recommendations": entry.put("recommendations", ActionSpelFunctions.cleanRuleDescription(readString(reader))); break;
                case "Tips": addTips(reader, tips); break;
                case "References": addReferences(reader, references); break;
                }
            });
            return consumer.apply(entry);
        }
        
        @SneakyThrows
        private void addTips(XMLStreamReader reader, ArrayNode tips) {
            processElement(reader, name->{
                switch ( name ) {
                case "Tip": tips.add(ActionSpelFunctions.cleanRuleDescription(readString(reader)));
                }
            });
        }
        
        @SneakyThrows
        private void addReferences(XMLStreamReader reader, ArrayNode references) {
            processElement(reader, name->{
                switch ( name ) {
                case "Reference": references.add(readReference(reader));
                }
            });
        }
        
        @SneakyThrows
        private ObjectNode readReference(XMLStreamReader reader) {
            var reference = JsonHelper.getObjectMapper().createObjectNode();
            processElement(reader, name->{
                switch ( name ) {
                case "Title": reference.put("title", readString(reader)); break;
                case "Publisher": reference.put("publisher", readString(reader)); break;
                case "Author": reference.put("author", readString(reader)); break;
                case "Source": reference.put("source", readString(reader)); break;
                }
            });
            return reference;
        }

        private void processElement(XMLStreamReader reader, Consumer<String> consumer) throws XMLStreamException {
            int level = 1;
            while(level > 0 && reader.hasNext()) {
                int eventType = reader.next();
                switch ( eventType ) {
                case XMLStreamReader.START_ELEMENT:
                    level++;
                    consumer.accept(reader.getLocalName());
                case XMLStreamReader.END_ELEMENT:
                    level--; break;
                }
            }
        }

        @SneakyThrows
        private String readString(XMLStreamReader reader) {
            StringBuilder result = new StringBuilder();
            while (reader.hasNext()) {
                int eventType = reader.next();
                switch (eventType) {
                    case XMLStreamReader.CHARACTERS:
                    case XMLStreamReader.CDATA:
                        result.append(reader.getText());
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        return result.toString();
                }
            }
            throw new XMLStreamException("Premature end of file");
        }
    }
    
}