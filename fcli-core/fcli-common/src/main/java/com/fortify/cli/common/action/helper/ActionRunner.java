/**
 * Copyright 2023 Open Text.
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
import java.util.concurrent.Callable;
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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.action.helper.ActionDescriptor.AbstractActionForEachDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionParameterDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionRequestTargetDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepAppendDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepFcliDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepForEachDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepForEachDescriptor.IActionStepForEachProcessor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestDescriptor.ActionStepRequestForEachDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestDescriptor.ActionStepRequestPagingProgressDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepRequestDescriptor.ActionStepRequestType;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepSetDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepUnsetDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionStepWriteDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionValidationException;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionValueTemplateDescriptor;
import com.fortify.cli.common.action.helper.ActionDescriptor.IActionIfSupplier;
import com.fortify.cli.common.action.helper.ActionDescriptor.IActionValueSupplier;
import com.fortify.cli.common.action.helper.ActionRunner.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.action.helper.ActionRunner.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.cli.util.FcliCommandExecutor;
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
import picocli.CommandLine;

@Builder
public class ActionRunner implements AutoCloseable {
    /** Save original stdout for delayed output operations */
    private static final PrintStream stdout = System.out;
    /** Save original stderr for delayed output operations */
    private static final PrintStream stderr = System.err;
    /** Jackson {@link ObjectMapper} used for various JSON-related operations */
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    /** Jackson {@link ObjectMapper} used for formatting steps in logging/exception messages */
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionRunner.class);
    /** Progress writer, provided through builder method */
    private final IProgressWriterI18n progressWriter;
    /** Root CommandLine object for executing fcli commands, provided through builder method */
    private final CommandLine rootCommandLine;
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
    @Builder.Default private int exitCode = 0;
    @Builder.Default private boolean exitRequested = false;
    
    public final Callable<Integer> run(String[] args) {
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
        return ()->{
            delayedConsoleWriterRunnables.forEach(Runnable::run);
            return exitCode;
        };
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
    
    public IActionRequestHelper getRequestHelper(String name) {
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
    
    private final class ActionStepsProcessor {
        private final ObjectNode localData;
        private final ActionStepsProcessor parent;
        
        public ActionStepsProcessor(ObjectNode localData, ActionStepsProcessor parent) {
            this.localData = localData;
            this.parent = parent;
        }

        private final void processSteps() {
            processSteps(action.getSteps());
        }
        
        private final void processSteps(List<ActionStepDescriptor> steps) {
            if ( steps!=null ) { steps.forEach(this::processStep); }
        }
        
        private final void processStep(ActionStepDescriptor step) {
            if ( _if(step) ) {
                processStepSupplier(step::getProgress, this::processProgressStep);
                processStepSupplier(step::getWarn, this::processWarnStep);
                processStepSupplier(step::getDebug, this::processDebugStep);
                processStepSupplier(step::get_throw, this::processThrowStep);
                processStepSupplier(step::get_exit, this::processExitStep);
                processStepSupplier(step::getRequests, this::processRequestsStep);
                processStepSupplier(step::getForEach, this::processForEachStep);
                processStepEntries(step::getFcli, this::processFcliStep);
                processStepEntries(step::getSet, this::processSetStep);
                processStepEntries(step::getAppend, this::processAppendStep);
                processStepEntries(step::getUnset, this::processUnsetStep);
                processStepEntries(step::getWrite, this::processWriteStep);
                processStepEntries(step::getSteps, this::processStep);
            }
        }
        
        private <T> void processStepEntries(Supplier<List<T>> supplier, Consumer<T> consumer) {
            var list = supplier.get();
            if ( list!=null ) { list.forEach(value->processStep(value, consumer)); }
        }
        
        private <T> void processStepSupplier(Supplier<T> supplier, Consumer<T> consumer) {
            processStep(supplier.get(), consumer);
        }
        
        private <T> void processStep(T value, Consumer<T> consumer) {
            if ( _if(value) ) {
                String valueString = null;
                if ( LOG.isDebugEnabled() ) {
                    valueString = getStepAsString(valueString, value);
                    LOG.debug("Start processing:\n"+valueString);
                }
                try {
                    consumer.accept(value);
                } catch ( Exception e ) {
                    if ( e instanceof StepProcessingException ) {
                        throw e;
                    } else {
                        valueString = getStepAsString(valueString, value);
                        throw new StepProcessingException("Error processing:\n"+valueString, e);
                    }
                }
                if ( LOG.isDebugEnabled() ) {
                    valueString = getStepAsString(valueString, value);
                    LOG.debug("End processing:\n"+valueString);
                }
            }
        }
        
        private final String getStepAsString(String cachedString, Object value) {
            if ( value==null ) { return null; }
            if ( cachedString!=null ) { return cachedString; }
            try {
                cachedString = String.format("%s:\n%s", 
                    StringUtils.indent(value.getClass().getCanonicalName(), "  "),
                    StringUtils.indent(yamlObjectMapper.valueToTree(value).toPrettyString(), "    "));
            } catch ( Exception e ) {
                cachedString = StringUtils.indent(value.toString(), "  ");
            }
            return cachedString;
        }
        
        private final boolean _if(Object o) {
            if (exitRequested || o==null) { return false; }
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
            var value = getValue(set);
            setDataValue(name, value);
        }
        
        private void processAppendStep(ActionStepAppendDescriptor append) {
            var name = append.getName();
            var property = append.getProperty();
            var currentValue = localData.get(name);
            var valueToAppend = getValue(append);
            if ( property==null ) {
                appendToArray(name, currentValue, valueToAppend);
            } else {
                appendToObject(name, currentValue, spelEvaluator.evaluate(property, localData, String.class), valueToAppend);
            }
        }

        private void appendToArray(String name, JsonNode currentValue, JsonNode valueToAppend) {
            if ( currentValue==null ) {
                currentValue = objectMapper.createArrayNode();
            }
            if ( !currentValue.isArray() ) {
                throw new IllegalStateException("Cannot append value to non-array node "+currentValue.getNodeType());
            } else {
                if ( valueToAppend!=null ) {
                    ((ArrayNode)currentValue).add(valueToAppend);
                }
                setDataValue(name, currentValue); // Update copies in parents
            }
        }
        
        private void appendToObject(String name, JsonNode currentValue, String property, JsonNode valueToAppend) {
            if ( currentValue==null ) {
                currentValue = objectMapper.createObjectNode();
            }
            if ( !currentValue.isObject() ) {
                throw new IllegalStateException(String.format("Cannot append value to non-object node "+currentValue.getNodeType()));
            } else {
                if ( valueToAppend!=null ) {
                    ((ObjectNode)currentValue).set(property, valueToAppend);
                }
                setDataValue(name, currentValue); // Update copies in parents
            }
        }

        private void processUnsetStep(ActionStepUnsetDescriptor unset) {
            unsetDataValue(unset.getName());
        }

        private void setDataValue(String name, JsonNode value) {
            localData.set(name, value);
            if ( parent!=null ) { parent.setDataValue(name, value); }
        }
        
        private void unsetDataValue(String name) {
            localData.remove(name);
            if ( parent!=null ) { parent.unsetDataValue(name); }
        }
        
        private JsonNode getValue(IActionValueSupplier supplier) {
            var value = supplier.getValue();
            var valueTemplate = supplier.getValueTemplate();
            if ( value!=null ) { return getValue(value); }
            else if ( StringUtils.isNotBlank(valueTemplate) ) { return getTemplateValue(valueTemplate); }
            else { throw new IllegalStateException("Either value or valueTemplate must be specified"); }
        }

        private JsonNode getValue(TemplateExpression valueExpression) {
            var value = spelEvaluator.evaluate(valueExpression, localData, Object.class);
            return objectMapper.valueToTree(value);
        }
        
        private final JsonNode getTemplateValue(String templateName) {
            var valueTemplateDescriptor = action.getValueTemplatesByName().get(templateName);
            var outputRawContents = valueTemplateDescriptor.getContents();
            return new JsonNodeOutputWalker(spelEvaluator, valueTemplateDescriptor, localData).walk(outputRawContents);
        }
        
        private void processWriteStep(ActionStepWriteDescriptor write) {
            var to = spelEvaluator.evaluate(write.getTo(), localData, String.class);
            var value = asString(getValue(write));
            try {
                switch (to.toLowerCase()) {
                case "stdout": delayedConsoleWriterRunnables.add(createRunner(stdout, value)); break;
                case "stderr": delayedConsoleWriterRunnables.add(createRunner(stderr, value)); break;
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
            throw new StepProcessingException(spelEvaluator.evaluate(message, localData, String.class));
        }
        
        private void processExitStep(TemplateExpression exitCodeExpression) {
            exitCode = spelEvaluator.evaluate(exitCodeExpression, localData, Integer.class);
            exitRequested = true;
        }
        
        private void processForEachStep(ActionStepForEachDescriptor forEach) {
            var processorExpression = forEach.getProcessor();
            var valuesExpression = forEach.getValues();
            if ( processorExpression!=null ) {
                var processor = spelEvaluator.evaluate(processorExpression, localData, IActionStepForEachProcessor.class);
                if ( processor!=null ) { processor.process(node->processForEachStepNode(forEach, node)); }
            } else if ( valuesExpression!=null ) {
                var values = spelEvaluator.evaluate(valuesExpression, localData, ArrayNode.class);
                if ( values!=null ) { JsonHelper.stream(values).takeWhile(value->processForEachStepNode(forEach, value)); }
            }
        }
        
        private boolean processForEachStepNode(AbstractActionForEachDescriptor forEach, JsonNode node) {
            if ( forEach==null ) { return false; }
            var breakIf = forEach.getBreakIf();
            setDataValue(forEach.getName(), node);
            if ( breakIf!=null && spelEvaluator.evaluate(breakIf, localData, Boolean.class) ) {
                return false;
            }
            if ( _if(forEach) ) {
                processSteps(forEach.get_do());
            }
            return true;
        }
        
        // TODO Handle fcli::name
        private void processFcliStep(ActionStepFcliDescriptor fcli) {
            var cmd = spelEvaluator.evaluate(fcli.getCmd(), localData, String.class);
            progressWriter.writeProgress("Executing fcli %s", cmd);
            var cmdExecutor = new FcliCommandExecutor(rootCommandLine, cmd);
            Consumer<ObjectNode> recordConsumer = null;
            var forEach = fcli.getForEach();
            var name = fcli.getName();
            if ( forEach!=null || StringUtils.isNotBlank(name) ) {
                if ( !cmdExecutor.canCollectRecords() ) {
                    throw new IllegalStateException("Can't use forEach or name on fcli command: "+cmd);
                } else {
                    recordConsumer = new FcliRecordConsumer(fcli);
                }
            }
            
            // TODO Implement optional output suppression
            var output = cmdExecutor.execute(recordConsumer, true);
            delayedConsoleWriterRunnables.add(createRunner(System.err, output.getErr()));
            delayedConsoleWriterRunnables.add(createRunner(System.out, output.getOut()));
            if ( output.getExitCode() >0 ) { 
                throw new StepProcessingException("Fcli command returned non-zero exit code "+output.getExitCode()); 
            }
        }
        @RequiredArgsConstructor
        private class FcliRecordConsumer implements Consumer<ObjectNode> {
            private final ActionStepFcliDescriptor fcli;
            private boolean continueProcessing = true;
            @Override
            public void accept(ObjectNode record) {
                var name = fcli.getName();
                if ( StringUtils.isNotBlank(name) ) {
                    // For name attribute, we want to collect all records,
                    // independent of break condition in the forEach block.
                    appendToArray(name, localData.get(name), record);
                }
                if ( continueProcessing ) {
                    continueProcessing = processForEachStepNode(fcli.getForEach(), record);
                }
            }
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
            processRequestStepForEach(requestDescriptor);
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
    
        private final void processRequestStepForEach(ActionStepRequestDescriptor requestDescriptor) {
            var forEach = requestDescriptor.getForEach();
            if ( forEach!=null ) {
                var input = localData.get(requestDescriptor.getName());
                if ( input!=null ) {
                    if ( input instanceof ArrayNode ) {
                        updateRequestStepForEachTotalCount(forEach, (ArrayNode)input);
                        processRequestStepForEachEmbed(forEach, (ArrayNode)input);
                        processRequestStepForEach(forEach, (ArrayNode)input, this::processRequestStepForEachEntryDo);
                    } else {
                        throw new ActionValidationException("forEach not supported on node type "+input.getNodeType());
                    }
                }
            }
        }
        
        private final void processRequestStepForEachEmbed(ActionStepRequestForEachDescriptor forEach, ArrayNode source) {
            var requestExecutor = new ActionStepRequestsProcessor();
            processRequestStepForEach(forEach, source, getRequestForEachEntryEmbedProcessor(requestExecutor));
            requestExecutor.executeRequests();
        }
        
        @FunctionalInterface
        private interface IRequestStepForEachEntryProcessor {
            void process(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ObjectNode newData);
        }
        
        private final void processRequestStepForEach(ActionStepRequestForEachDescriptor forEach, ArrayNode source, IRequestStepForEachEntryProcessor entryProcessor) {
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
        
        private void updateRequestStepForEachTotalCount(ActionStepRequestForEachDescriptor forEach, ArrayNode array) {
            var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getName()));
            var totalCount = localData.get(totalCountName);
            if ( totalCount==null ) { totalCount = new IntNode(0); }
            localData.put(totalCountName, totalCount.asInt()+array.size());
        }

        private void processRequestStepForEachEntryDo(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ObjectNode newData) {
            var processor = new ActionStepsProcessor(newData, this);
            processor.processSteps(forEach.get_do());
        }
        
        private IRequestStepForEachEntryProcessor getRequestForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
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
                var method = requestDescriptor.getMethod();
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
        public UnirestInstance getUnirestInstance();
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
    
    public static final class StepProcessingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StepProcessingException(String message, Throwable cause) {
            super(message, cause);
        }

        public StepProcessingException(String message) {
            super(message);
        }

        public StepProcessingException(Throwable cause) {
            super(cause);
        }
    }
}
