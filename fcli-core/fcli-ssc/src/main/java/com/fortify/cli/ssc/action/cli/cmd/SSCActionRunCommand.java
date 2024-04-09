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
package com.fortify.cli.ssc.action.cli.cmd;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.cli.cmd.AbstractActionRunCommand;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepForEachDescriptor.IActionStepForEachProcessor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionValidationException;
import com.fortify.cli.common.action.helper.ActionRunner;
import com.fortify.cli.common.action.helper.ActionRunner.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.action.helper.ActionRunner.ParameterTypeConverterArgs;
import com.fortify.cli.common.action.helper.ActionSpelFunctions;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.helper.SSCProductHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.SSCFileTransferTokenSupplier;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.SSCFileTransferTokenType;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetHelper;

import kong.unirest.HttpRequest;
import kong.unirest.RawResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "run")
public class SSCActionRunCommand extends AbstractActionRunCommand {
    @Getter @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;
    
    @Override
    protected final String getType() {
        return "SSC";
    }
    
    @Override
    protected void configure(ActionRunner templateRunner, SimpleEvaluationContext context) {
        templateRunner
            .addParameterConverter("appversion_single", this::loadAppVersion)
            .addParameterConverter("filterset", this::loadFilterSet)
            .addRequestHelper("ssc", new SSCActionRequestHelper(unirestInstanceSupplier::getUnirestInstance, SSCProductHelper.INSTANCE));
        context.setVariable("ssc", new SSCSpelFunctions(templateRunner));
    }
    
    @RequiredArgsConstructor @Reflectable
    public final class SSCSpelFunctions {
        private final ActionRunner templateRunner;
        public IActionStepForEachProcessor ruleDescriptionsProcessor(String appVersionId) {
            var unirest = templateRunner.getRequestHelper("ssc").getUnirestInstance();
            return new SSCFPRRuleDescriptionProcessor(unirest, appVersionId)::process;
        }
        public String issueBrowserUrl(ObjectNode issue, ObjectNode filterset) {
            var deepLinkExpression = baseUrl()
                    +"/html/ssc/version/${projectVersionId}/fix/${id}/?engineType=${engineType}&issue=${issueInstanceId}";
            if ( filterset!=null ) { 
                deepLinkExpression+="&filterSet="+filterset.get("guid").asText();
            }
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue, String.class);
        }
        public String appversionBrowserUrl(ObjectNode appversion) {
            var deepLinkExpression = baseUrl()
                    +"/html/ssc/index.jsp#!/version/${id}/fix";
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
        }
        private String baseUrl() {
            return unirestInstanceSupplier.getSessionDescriptor().getUrlConfig().getUrl()
                    .replaceAll("/+$", "");
        }
        
    }
    
    private final JsonNode loadAppVersion(String nameOrId, ParameterTypeConverterArgs args) {
        args.getProgressWriter().writeProgress("Loading application version %s", nameOrId);
        var result = SSCAppVersionHelper.getRequiredAppVersion(unirestInstanceSupplier.getUnirestInstance(), nameOrId, ":");
        args.getProgressWriter().writeProgress("Loaded application version %s", result.getAppAndVersionName());
        return result.asJsonNode();
    }
    
    private final JsonNode loadFilterSet(String titleOrId, ParameterTypeConverterArgs args) {
        var progressMessage = StringUtils.isBlank(titleOrId) 
                ? "Loading default filter set" 
                : String.format("Loading filter set %s", titleOrId);
        args.getProgressWriter().writeProgress(progressMessage);
        var parameter = args.getParameter();
        var typeParameters = parameter.getTypeParameters();
        var appVersionIdExpression = typeParameters==null ? null : typeParameters.get("appversion.id");
        if ( appVersionIdExpression==null ) {
            appVersionIdExpression = SpelHelper.parseTemplateExpression("${appversion?.id}");
        }
        var appVersionId = args.getSpelEvaluator().evaluate(appVersionIdExpression, args.getParameters(), String.class);
        if ( StringUtils.isBlank(appVersionId) ) {
            throw new ActionValidationException(String.format("Template parameter %s requires ${%s} to be available", parameter.getName(), appVersionIdExpression.getExpressionString()));
        }
        var filterSetDescriptor = new SSCIssueFilterSetHelper(unirestInstanceSupplier.getUnirestInstance(), appVersionId).getDescriptorByTitleOrId(titleOrId, false);
        if ( filterSetDescriptor==null ) {
            throw new IllegalArgumentException("Unknown filter set: "+titleOrId);
        }
        return filterSetDescriptor.asJsonNode();
    }
    
    private static final class SSCActionRequestHelper extends BasicActionRequestHelper {
        public SSCActionRequestHelper(IUnirestInstanceSupplier unirestInstanceSupplier, IProductHelper productHelper) {
            super(unirestInstanceSupplier, productHelper);
        }

        @Override
        public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptors) {
            if ( requestDescriptors.size()==1 ) {
                var rd = requestDescriptors.get(0);
                createRequest(rd).asObject(JsonNode.class).ifSuccess(r->rd.getResponseConsumer().accept(r.getBody()));
            } else {
                var bulkRequestBuilder = new SSCBulkRequestBuilder();
                requestDescriptors.forEach(r->bulkRequestBuilder.request(createRequest(r), r.getResponseConsumer()));
                bulkRequestBuilder.execute(getUnirestInstance());
            }
        }
        
        private HttpRequest<?> createRequest(ActionRequestDescriptor requestDescriptor) {
            var request = getUnirestInstance().request(requestDescriptor.getMethod(), requestDescriptor.getUri())
                    .queryString(requestDescriptor.getQueryParams());
            var body = requestDescriptor.getBody();
            return body==null ? request : request.body(body);
        }
    }
    
    @RequiredArgsConstructor
    private final class SSCFPRRuleDescriptionProcessor {
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
