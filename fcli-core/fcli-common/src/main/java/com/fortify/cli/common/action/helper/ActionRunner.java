/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.helper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionParameterDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionRequestTargetDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepForEachDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestPagingProgressDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestType;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepSetDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepSetOperation;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepWriteDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionValidationException;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionValueTemplateDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.IActionIfSupplier;
import com.fortify.cli.common.action.helper.ActionRunner.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.action.helper.ActionRunner.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.cli.util.SimpleOptionsParser;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonHelper.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spring.expression.ISpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
public class ActionRunner implements AutoCloseable {
    /** Jackson {@link ObjectMapper} used for various JSON-related operations */
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionRunner.class);
    /** Progress writer, provided through builder method */
    private final IProgressWriterI18n progressWriter;
    /** Data extract action, provided through builder method */
    @Getter private final ActionDescriptor action;
    /** Callback to handle validation errors */
    private final Function<OptionsParseResult, RuntimeException> onValidationErrors; 
    /** ObjectNode holding global data values as produced by the various steps */
    @Getter private final ObjectNode globalData = objectMapper.createObjectNode();
    /** ObjectNode holding parameter values as generated by ActionParameterProcessor */
    @Getter private final ObjectNode parameters = objectMapper.createObjectNode();
    /** SpEL evaluator configured with {@link ActionSpelFunctions} and variables for
     *  parameters, partialOutputs and outputs as defined above */
    @Getter private final IConfigurableSpelEvaluator spelEvaluator = SpelEvaluator.JSON_GENERIC.copy().configure(this::configureSpelEvaluator);
    /** Parameter converters as generated by {@link #createDefaultParameterConverters()} amended by 
     *  custom converters as added through the {@link #addParameterConverter(String, BiFunction)} and
     *  {@link #addParameterConverter(String, Function)} methods. */
    private final Map<String, BiFunction<String, ParameterTypeConverterArgs, JsonNode>> parameterConverters = createDefaultParameterConverters();
    /** Request helpers as configured through the {@link #addRequestHelper(String, IActionRequestHelper)} method */
    private final Map<String, IActionRequestHelper> requestHelpers = new HashMap<>();
    // We need to delay writing output to console as to not interfere with progress writer
    private final List<Runnable> delayedConsoleWriterRunnables = new ArrayList<>();
    
    public final Runnable run(String[] args) {
        globalData.set("parameters", parameters);
        progressWriter.writeProgress("Processing action parameters");
        var optionsParseResult = new ActionParameterProcessor(args).processParameters();
        if ( optionsParseResult.hasValidationErrors() ) {
            throw onValidationErrors.apply(optionsParseResult);
        } else {
            new ActionAddRequestTargetsProcessor().addRequestTargets();
            progressWriter.writeProgress("Processing action steps");
            new ActionStepsProcessor(globalData, null).processSteps();
            progressWriter.writeProgress("Producing action outputs");
            progressWriter.writeProgress("Action processing finished");
        }
        return ()->delayedConsoleWriterRunnables.forEach(Runnable::run);
    }

    public final void close() {
        requestHelpers.values().forEach(IActionRequestHelper::close);
    }
    
    private final void configureSpelEvaluator(SimpleEvaluationContext context) {
        SpelHelper.registerFunctions(context, ActionSpelFunctions.class);
    }
    
    public final ActionRunner addParameterConverter(String type, BiFunction<String, ParameterTypeConverterArgs, JsonNode> converter) {
        parameterConverters.put(type, converter);
        return this;
    }
    public final ActionRunner addParameterConverter(String type, Function<String, JsonNode> converter) {
        parameterConverters.put(type, (v,a)->converter.apply(v));
        return this;
    }
    public final ActionRunner addRequestHelper(String name, IActionRequestHelper requestHelper) {
        requestHelpers.put(name, requestHelper);
        return this;
    }
    
    private IActionRequestHelper getRequestHelper(String name) {
        if ( StringUtils.isBlank(name) ) {
            if ( requestHelpers.size()==1 ) {
                return requestHelpers.values().iterator().next();
            } else {
                throw new IllegalStateException(String.format("Required 'from:' property (allowed values: %s) missing", requestHelpers.keySet()));
            }
        } 
        var result = requestHelpers.get(name);
        if ( result==null ) {
            throw new IllegalStateException(String.format("Invalid 'from: %s', allowed values: %s", name, requestHelpers.keySet()));
        }
        return result;
    }
    
    private final <T> Map<String, T> evaluateTemplateExpressionMap(Map<String, TemplateExpression> queryExpressions, ObjectNode data, Class<T> targetClass) {
        Map<String, T> result = new LinkedHashMap<>();
        if ( queryExpressions!=null ) {
            queryExpressions.entrySet().forEach(e->result.put(e.getKey(), spelEvaluator.evaluate(e.getValue(), data, targetClass)));
        }
        return result;
    }
    
    private final class ActionParameterProcessor {
        private final OptionsParseResult optionsParseResult;

        public ActionParameterProcessor(String[] args) {
            this.optionsParseResult = parseParameterValues(args);
        }
        private final OptionsParseResult processParameters() {
            var validationErrors = optionsParseResult.getValidationErrors();
            if ( validationErrors.size()==0 ) {
                action.getParameters().forEach(this::addParameterData);
            }
            return optionsParseResult;
        }
        
        private final OptionsParseResult parseParameterValues(String[] args) {
            List<IOptionDescriptor> optionDescriptors = ActionParameterHelper.getOptionDescriptors(action);
            var parseResult = new SimpleOptionsParser(optionDescriptors).parse(args);
            addDefaultValues(parseResult);
            addValidationMessages(parseResult);
            return parseResult;
        }

        private final void addDefaultValues(OptionsParseResult parseResult) {
            action.getParameters().forEach(p->addDefaultValue(parseResult, p));
        }
        
        private final void addValidationMessages(OptionsParseResult parseResult) {
            action.getParameters().forEach(p->addValidationMessages(parseResult, p));
        }
        
        private final void addDefaultValue(OptionsParseResult parseResult, ActionParameterDescriptor parameter) {
            var name = parameter.getName();
            var value = getOptionValue(parseResult, parameter);
            if ( value==null ) {
                var defaultValueExpression = parameter.getDefaultValue();
                value = defaultValueExpression==null 
                        ? null 
                        : spelEvaluator.evaluate(defaultValueExpression, globalData, String.class);
            }
            parseResult.getOptionValuesByName().put(ActionParameterHelper.getOptionName(name), value);
        }
        
        private final void addValidationMessages(OptionsParseResult parseResult, ActionParameterDescriptor parameter) {
            if ( parameter.isRequired() && StringUtils.isBlank(getOptionValue(parseResult, parameter)) ) {
                parseResult.getValidationErrors().add("No value provided for required option "+
                        ActionParameterHelper.getOptionName(parameter.getName()));                
            }
        }

        private final void addParameterData(ActionParameterDescriptor parameter) {
            var name = parameter.getName();
            var value = getOptionValue(optionsParseResult, parameter);
            if ( value==null ) {
                var defaultValueExpression = parameter.getDefaultValue();
                value = defaultValueExpression==null 
                        ? null 
                        : spelEvaluator.evaluate(defaultValueExpression, globalData, String.class);
            }
            parameters.set(name, convertParameterValue(value, parameter));
        }
        private String getOptionValue(OptionsParseResult parseResult, ActionParameterDescriptor parameter) {
            var optionName = ActionParameterHelper.getOptionName(parameter.getName());
            return parseResult.getOptionValuesByName().get(optionName);
        }
        
        private JsonNode convertParameterValue(String value, ActionParameterDescriptor parameter) {
            var name = parameter.getName();
            var type = StringUtils.isBlank(parameter.getType()) ? "string" : parameter.getType();
            var paramConverter = parameterConverters.get(type);
            if ( paramConverter==null ) {
                throw new ActionValidationException(String.format("Unknown parameter type %s for parameter %s", type, name)); 
            } else {
                var args = ParameterTypeConverterArgs.builder()
                        .progressWriter(progressWriter)
                        .spelEvaluator(spelEvaluator)
                        .action(action)
                        .parameter(parameter)
                        .parameters(parameters)
                        .build();
                var result = paramConverter.apply(value, args);
                return result==null ? NullNode.instance : result; 
            }
        }  
    }
    
    private final class ActionAddRequestTargetsProcessor {
        private final void addRequestTargets() {
            var requestTargets = action.getAddRequestTargets();
            if ( requestTargets!=null ) {
                requestTargets.forEach(this::addRequestTarget);
            }
        }
        private void addRequestTarget(ActionRequestTargetDescriptor descriptor) {
            requestHelpers.put(descriptor.getName(), createBasicRequestHelper(descriptor));
        }
        
        private IActionRequestHelper createBasicRequestHelper(ActionRequestTargetDescriptor descriptor) {
            var name = descriptor.getName();
            var baseUrl = spelEvaluator.evaluate(descriptor.getBaseUrl(), globalData, String.class);
            var headers = evaluateTemplateExpressionMap(descriptor.getHeaders(), globalData, String.class);
            IUnirestInstanceSupplier unirestInstanceSupplier = () -> GenericUnirestFactory.getUnirestInstance(name, u->{
                u.config().defaultBaseUrl(baseUrl).getDefaultHeaders().add(headers);
                UnirestUnexpectedHttpResponseConfigurer.configure(u);
                UnirestJsonHeaderConfigurer.configure(u);
            });
            return new BasicActionRequestHelper(unirestInstanceSupplier, null);
        }
    }
    
    @RequiredArgsConstructor
    private final class ActionStepsProcessor {
        private final ObjectNode localData;
        private final ActionStepsProcessor parent;
        
        private final void processSteps() {
            processSteps(action.getSteps());
        }
        
        private final void processSteps(List<ActionStepDescriptor> steps) {
            if ( steps!=null ) { steps.forEach(this::processStep); }
        }
        
        private final void processStep(ActionStepDescriptor step) {
            if ( _if(step) ) {
                processSupplier(step::getProgress, this::processProgressStep);
                processSupplier(step::getWarn, this::processWarnStep);
                processSupplier(step::getDebug, this::processDebugStep);
                processSupplier(step::get_throw, this::processThrowStep);
                processSupplier(step::getRequests, this::processRequestsStep);
                processAll(step::getSet, this::processSetStep);
                processAll(step::getWrite, this::processWriteStep);
            }
        }
        
        private <T> void processAll(Supplier<List<T>> supplier, Consumer<T> consumer) {
            var list = supplier.get();
            if ( list!=null ) { list.forEach(value->processValue(value, consumer)); }
        }
        
        private <T> void processSupplier(Supplier<T> supplier, Consumer<T> consumer) {
            processValue(supplier.get(), consumer);
        }
        
        private <T> void processValue(T value, Consumer<T> consumer) {
            if ( _if(value) ) { consumer.accept(value); }
        }
        
        private final boolean _if(Object o) {
            if (o==null) { return false; }
            if (o instanceof IActionIfSupplier ) {
                var _if = ((IActionIfSupplier) o).get_if();
                if ( _if!=null ) {
                    return spelEvaluator.evaluate(_if, localData, Boolean.class);
                }
            }
            return true;
        }
        
        private void processSetStep(ActionStepSetDescriptor set) {
            var name = set.getName();
            var value = getValueForSetStep(set);
            setDataValue(name, value);
        }

        private void setDataValue(String name, JsonNode value) {
            localData.set(name, value);
            if ( parent!=null ) { parent.setDataValue(name, value); }
        }

        private JsonNode getValueForSetStep(ActionStepSetDescriptor set) {
            var valueExpression = set.getValue();
            var valueTemplate = set.getValueTemplate();
            var value = valueExpression!=null 
                    ? getValue(valueExpression)
                    : getFormattedValue(valueTemplate);
            return getTargetValueForSetStep(set, value);
        }
        
        private JsonNode getTargetValueForSetStep(ActionStepSetDescriptor set, JsonNode value) {
            var result = value;
            var name = set.getName();
            var op = set.getOperation();
            if ( op==ActionStepSetOperation.append ) {
                result = localData.get(name);
                if ( result==null ) {
                    result = objectMapper.createArrayNode();
                }
                if ( !result.isArray() ) {
                    throw new IllegalStateException("Cannot append value to existing value of type "+result.getNodeType());
                }
                ((ArrayNode)result).add(value);
            }
            return result;
        }

        private JsonNode getValue(TemplateExpression valueExpression) {
            var value = spelEvaluator.evaluate(valueExpression, localData, Object.class);
            return objectMapper.valueToTree(value);
        }

        private JsonNode getFormattedValue(String valueTemplate) {
            var valueTemplateDescriptor = action.getValueTemplatesByName().get(valueTemplate);
            var outputRawContents = valueTemplateDescriptor.getContents();
            return new JsonNodeOutputWalker(spelEvaluator, valueTemplateDescriptor, localData).walk(outputRawContents);
        }
        
        private void processWriteStep(ActionStepWriteDescriptor write) {
            var to = spelEvaluator.evaluate(write.getTo(), localData, String.class);
            var valueExpression = write.getValue();
            var valueTemplate = write.getValueTemplate();
            var value = asString(valueExpression!=null 
                    ? getValue(valueExpression)
                    : getFormattedValue(valueTemplate));
            try {
                switch (to.toLowerCase()) {
                case "stdout": delayedConsoleWriterRunnables.add(createRunner(System.out, value)); break;
                case "stderr": delayedConsoleWriterRunnables.add(createRunner(System.err, value)); break;
                default: write(new File(to), value);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error writing action output to "+to);
            }
        }
        
        private Runnable createRunner(PrintStream out, String output) {
            return ()->out.print(output);
        }

        private void write(File file, String output) throws IOException {
            try ( var out = new PrintStream(file, StandardCharsets.UTF_8) ) {
                out.println(output);
            }
        }

        private final String asString(Object output) {
            if ( output instanceof TextNode ) {
                return ((TextNode)output).asText();
            } else if ( output instanceof JsonNode ) {
                return ((JsonNode)output).toPrettyString();
            } else {
                return output.toString();
            }
        }  

        private void processProgressStep(TemplateExpression progress) {
            progressWriter.writeProgress(spelEvaluator.evaluate(progress, localData, String.class));
        }
        
        private void processWarnStep(TemplateExpression progress) {
            progressWriter.writeWarning(spelEvaluator.evaluate(progress, localData, String.class));
        }
        
        private void processDebugStep(TemplateExpression progress) {
            LOG.debug(spelEvaluator.evaluate(progress, localData, String.class));
        }
        
        private void processThrowStep(TemplateExpression message) {
            throw new RuntimeException(spelEvaluator.evaluate(message, localData, String.class));
        }
        
        private void processRequestsStep(List<ActionStepRequestDescriptor> requests) {
            if ( requests!=null ) {
                var requestsProcessor = new ActionStepRequestsProcessor();
                requestsProcessor.addRequests(requests, this::processResponse, this::processFailure, localData);
                requestsProcessor.executeRequests();
            }
        }
        
        private final void processResponse(ActionStepRequestDescriptor requestDescriptor, JsonNode rawBody) {
            var name = requestDescriptor.getName();
            var body = getRequestHelper(requestDescriptor.getTarget()).transformInput(rawBody);
            localData.set(name+"_raw", rawBody);
            localData.set(name, body);
            processOnResponse(requestDescriptor);
            processForEach(requestDescriptor);
        }
        
        private final void processFailure(ActionStepRequestDescriptor requestDescriptor, UnirestException e) {
            var onFailSteps = requestDescriptor.getOnFail();
            if ( onFailSteps==null ) { throw e; }
            localData.putPOJO("exception", e);
            processSteps(onFailSteps);
        }
        
        private final void processOnResponse(ActionStepRequestDescriptor requestDescriptor) {
            var onResponseSteps = requestDescriptor.getOnResponse();
            processSteps(onResponseSteps);
        }
    
        private final void processForEach(ActionStepRequestDescriptor requestDescriptor) {
            var forEach = requestDescriptor.getForEach();
            if ( forEach!=null ) {
                var input = localData.get(requestDescriptor.getName());
                if ( input!=null ) {
                    if ( input instanceof ArrayNode ) {
                        updateForEachTotalCount(forEach, (ArrayNode)input);
                        processForEachEmbed(forEach, (ArrayNode)input);
                        processForEach(forEach, (ArrayNode)input, this::processForEachEntryDo);
                    } else {
                        throw new ActionValidationException("forEach not supported on node type "+input.getNodeType());
                    }
                }
            }
        }
        
        private final void processForEachEmbed(ActionStepForEachDescriptor forEach, ArrayNode source) {
            var requestExecutor = new ActionStepRequestsProcessor();
            processForEach(forEach, source, getForEachEntryEmbedProcessor(requestExecutor));
            requestExecutor.executeRequests();
        }
        
        @FunctionalInterface
        private interface IForEachEntryProcessor {
            void process(ActionStepForEachDescriptor forEach, JsonNode currentNode, ObjectNode newData);
        }
        
        private final void processForEach(ActionStepForEachDescriptor forEach, ArrayNode source, IForEachEntryProcessor entryProcessor) {
            for ( int i = 0 ; i < source.size(); i++ ) {
                var currentNode = source.get(i);
                var newData = JsonHelper.shallowCopy(localData);
                newData.set(forEach.getName(), currentNode);
                var breakIf = forEach.getBreakIf();
                if ( breakIf!=null && spelEvaluator.evaluate(breakIf, newData, Boolean.class) ) {
                    break;
                }
                var _if = forEach.get_if(); 
                if ( _if==null || spelEvaluator.evaluate(_if, newData, Boolean.class) ) {
                    entryProcessor.process(forEach, currentNode, newData);
                }
            }
        }
        
        private void updateForEachTotalCount(ActionStepForEachDescriptor forEach, ArrayNode array) {
            var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getName()));
            var totalCount = localData.get(totalCountName);
            if ( totalCount==null ) { totalCount = new IntNode(0); }
            localData.put(totalCountName, totalCount.asInt()+array.size());
        }

        private void processForEachEntryDo(ActionStepForEachDescriptor forEach, JsonNode currentNode, ObjectNode newData) {
            var processor = new ActionStepsProcessor(newData, this);
            processor.processSteps(forEach.get_do());
        }
        
        private IForEachEntryProcessor getForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
            return (forEach, currentNode, newData) -> {
                if ( !currentNode.isObject() ) {
                    // TODO Improve exception message?
                    throw new IllegalStateException("Cannot embed data on non-object nodes: "+forEach.getName());
                }
                requestExecutor.addRequests(forEach.getEmbed(), 
                        (rd,r)->((ObjectNode)currentNode).set(rd.getName(), getRequestHelper(rd.getTarget()).transformInput(r)), 
                        this::processFailure, newData);
            };
        }
    }
    
    private final class ActionStepRequestsProcessor {
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> simpleRequests = new LinkedHashMap<>();
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> pagedRequests = new LinkedHashMap<>();
        
        private final void addRequests(List<ActionStepRequestDescriptor> requestDescriptors, BiConsumer<ActionStepRequestDescriptor, JsonNode> responseConsumer, BiConsumer<ActionStepRequestDescriptor, UnirestException> failureConsumer, ObjectNode data) {
            if ( requestDescriptors!=null ) {
                requestDescriptors.forEach(r->addRequest(r, responseConsumer, failureConsumer, data));
            }
        }
        
        private final void addRequest(ActionStepRequestDescriptor requestDescriptor, BiConsumer<ActionStepRequestDescriptor, JsonNode> responseConsumer, BiConsumer<ActionStepRequestDescriptor, UnirestException> failureConsumer, ObjectNode data) {
            var _if = requestDescriptor.get_if();
            if ( _if==null || spelEvaluator.evaluate(_if, data, Boolean.class) ) {
                var method = requestDescriptor.getMethod().toString();
                var uri = spelEvaluator.evaluate(requestDescriptor.getUri(), data, String.class);
                var query = evaluateTemplateExpressionMap(requestDescriptor.getQuery(), data, Object.class);
                var body = requestDescriptor.getBody()==null ? null : spelEvaluator.evaluate(requestDescriptor.getBody(), data, Object.class);
                var requestData = new IActionRequestHelper.ActionRequestDescriptor(method, uri, query, body, r->responseConsumer.accept(requestDescriptor, r), e->failureConsumer.accept(requestDescriptor, e));
                addPagingProgress(requestData, requestDescriptor.getPagingProgress(), data);
                if ( requestDescriptor.getType()==ActionStepRequestType.paged ) {
                    pagedRequests.computeIfAbsent(requestDescriptor.getTarget(), s->new ArrayList<IActionRequestHelper.ActionRequestDescriptor>()).add(requestData);
                } else {
                    simpleRequests.computeIfAbsent(requestDescriptor.getTarget(), s->new ArrayList<IActionRequestHelper.ActionRequestDescriptor>()).add(requestData);
                }
            }
        }

        private void addPagingProgress(ActionRequestDescriptor requestData, ActionStepRequestPagingProgressDescriptor pagingProgress, ObjectNode data) {
            if ( pagingProgress!=null ) {
                addPagingProgress(pagingProgress.getPrePageLoad(), requestData::setPrePageLoad, data);
                addPagingProgress(pagingProgress.getPostPageLoad(), requestData::setPostPageLoad, data);
                addPagingProgress(pagingProgress.getPostPageProcess(), requestData::setPostPageProcess, data);
            }
        }
        
        private void addPagingProgress(TemplateExpression expr, Consumer<Runnable> consumer, ObjectNode data) {
            if ( expr!=null ) {
                consumer.accept(()->progressWriter.writeProgress(spelEvaluator.evaluate(expr, data, String.class)));
            }
        }
        
        private final void executeRequests() {
            simpleRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), false));
            pagedRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), true));
        }
        
        private void executeRequest(String target, List<ActionRequestDescriptor> requests, boolean isPaged) {
            var requestHelper = getRequestHelper(target);
            if ( isPaged ) {
                requests.forEach(r->requestHelper.executePagedRequest(r));
            } else {
                requestHelper.executeSimpleRequests(requests);
            }
        }
    }
    
    public static interface IActionRequestHelper extends AutoCloseable {
        public JsonNode transformInput(JsonNode input);
        public void executePagedRequest(ActionRequestDescriptor requestDescriptor);
        public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptor);
        public void close();
        
        @Data
        public static final class ActionRequestDescriptor {
            private final String method;
            private final String uri;
            private final Map<String, Object> queryParams;
            private final Object body;
            private final Consumer<JsonNode> responseConsumer;
            private final Consumer<UnirestException> failureConsumer;
            private Runnable prePageLoad;
            private Runnable postPageLoad;
            private Runnable postPageProcess;
            
            public void prePageLoad() {
                run(prePageLoad);
            }
            public void postPageLoad() {
                run(postPageLoad);
            }
            public void postPageProcess() {
                run(postPageProcess);
            }
            private void run(Runnable runnable) {
                if ( runnable!=null ) { runnable.run(); }
            }
        }
        
        @RequiredArgsConstructor
        public static class BasicActionRequestHelper implements IActionRequestHelper {
            private final IUnirestInstanceSupplier unirestInstanceSupplier;
            private final IProductHelper productHelper;
            private UnirestInstance unirestInstance;
            public final UnirestInstance getUnirestInstance() {
                if ( unirestInstance==null ) {
                    unirestInstance = unirestInstanceSupplier.getUnirestInstance();
                }
                return unirestInstance;
            }
            
            @Override
            public JsonNode transformInput(JsonNode input) {
                return JavaHelper.as(productHelper, IInputTransformer.class).orElse(i->i).transformInput(input);
            }
            @Override
            public void executePagedRequest(ActionRequestDescriptor requestDescriptor) {
                var unirest = getUnirestInstance();
                INextPageUrlProducer nextPageUrlProducer = (req, resp)->{
                    var nextPageUrl = JavaHelper.as(productHelper, INextPageUrlProducerSupplier.class).get()
                            .getNextPageUrlProducer().getNextPageUrl(req, resp);
                    if ( nextPageUrl!=null ) {
                        requestDescriptor.prePageLoad();
                    }
                    return nextPageUrl;
                };
                HttpRequest<?> request = createRequest(unirest, requestDescriptor);
                requestDescriptor.prePageLoad();
                try {
                    PagingHelper.processPages(unirest, request, nextPageUrlProducer, r->{
                        requestDescriptor.postPageLoad();
                        requestDescriptor.getResponseConsumer().accept(r.getBody());
                        requestDescriptor.postPageProcess();
                    });
                } catch ( UnirestException e ) {
                    requestDescriptor.getFailureConsumer().accept(e);
                }
            }
            @Override
            public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptors) {
                var unirest = getUnirestInstance();
                requestDescriptors.forEach(r->executeSimpleRequest(unirest, r));
            }
            private void executeSimpleRequest(UnirestInstance unirest, ActionRequestDescriptor requestDescriptor) {
                try {
                    createRequest(unirest, requestDescriptor)
                        .asObject(JsonNode.class)
                        .ifSuccess(r->requestDescriptor.getResponseConsumer().accept(r.getBody()));
                } catch ( UnirestException e ) {
                    requestDescriptor.getFailureConsumer().accept(e);
                }
            }

            private HttpRequest<?> createRequest(UnirestInstance unirest, ActionRequestDescriptor r) {
                var result = unirest.request(r.getMethod(), r.getUri())
                    .queryString(r.getQueryParams());
                return r.getBody()==null ? result : result.body(r.getBody());
            }

            @Override
            public void close() {
                if ( unirestInstance!=null ) {
                    unirestInstance.close();
                }
            }
        }
    }
    
    @Builder @Data
    public static final class ParameterTypeConverterArgs {
        private final IProgressWriterI18n progressWriter;
        private final ISpelEvaluator spelEvaluator;
        private final ActionDescriptor action;
        private final ActionParameterDescriptor parameter;
        private final ObjectNode parameters;
    }
    
    private static final Map<String, BiFunction<String, ParameterTypeConverterArgs, JsonNode>> createDefaultParameterConverters() {
        Map<String, BiFunction<String, ParameterTypeConverterArgs, JsonNode>> result = new HashMap<>();
        result.put("string",  (v,a)->new TextNode(v));
        result.put("boolean", (v,a)->BooleanNode.valueOf(Boolean.parseBoolean(v)));
        result.put("int",     (v,a)->IntNode.valueOf(Integer.parseInt(v)));
        result.put("long",    (v,a)->LongNode.valueOf(Long.parseLong(v)));
        result.put("double",  (v,a)->DoubleNode.valueOf(Double.parseDouble(v)));
        result.put("float",   (v,a)->FloatNode.valueOf(Float.parseFloat(v)));
        // TODO Add BigIntegerNode/DecimalNode/ShortNode support?
        // TODO Add array support?
        return result;
    }
    
    @RequiredArgsConstructor
    private static final class JsonNodeOutputWalker extends JsonNodeDeepCopyWalker {
        private final ISpelEvaluator spelEvaluator;
        private final ActionValueTemplateDescriptor outputDescriptor;
        private final ObjectNode data;
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( !(node instanceof TextNode) ) {
                return super.copyValue(state, path, parent, node);
            } else {
                TemplateExpression expression = outputDescriptor.getValueExpressions().get(path);
                if ( expression==null ) { throw new RuntimeException("No expression for "+path); }
                try {
                    var rawResult = spelEvaluator.evaluate(expression, data, Object.class);
                    if ( rawResult instanceof CharSequence ) {
                        rawResult = new TextNode(((String)rawResult).replace("\\n", "\n"));
                    }
                    return objectMapper.valueToTree(rawResult);
                } catch ( SpelEvaluationException e ) {
                    throw new RuntimeException("Error evaluating action expression "+expression.getExpressionString(), e);
                }
            }
        }
    }
}
