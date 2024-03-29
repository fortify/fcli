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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.expression.ParseException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper.AbstractJsonNodeWalker;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.SimpleExpression;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpMethod;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class describes an action deserialized from an action YAML file, 
 * containing elements describing:
 * <ul> 
 *  <li>Action metadata like name and description</li> 
 *  <li>Action parameters</li>
 *  <li>Steps to be executed, like executing REST requests and writing output</li>
 *  <li>Data formatters</li>
 * </ul> 
 *
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data
public class ActionDescriptor {
    /** Action name, set in {@link #postLoad(String)} method */
    private String name;
    /** Whether this is a custom action, set in {@link #postLoad(String)} method */
    private boolean custom;
    /** Action description */
    private ActionUsageDescriptor usage;
    /** Action parameters */
    private List<ActionParameterDescriptor> parameters;
    /** Additional requests targets */
    private List<ActionRequestTargetDescriptor> addRequestTargets;
    /** Default values for certain action properties */
    private ActionDefaultValuesDescriptor defaults;
    /** Action steps, evaluated in the order as defined in the YAML file */
    private List<ActionStepDescriptor> steps;
    /** Value templates */
    private List<ActionValueTemplateDescriptor> valueTemplates;
    @JsonIgnore @Getter(lazy=true) private final Map<String, ActionValueTemplateDescriptor> valueTemplatesByName = 
            valueTemplates.stream().collect(Collectors.toMap(ActionValueTemplateDescriptor::getName, Function.identity()));
    
    /**
     * This method is invoked by ActionHelper after deserializing
     * an instance of this class from a YAML file. It performs some additional
     * initialization and validation.
     */
    final void postLoad(String name, boolean isCustom) {
        this.name = name;
        this.custom = isCustom;
        checkNotNull("action usage", usage, this);
        checkNotNull("action steps", steps, this);
        usage.postLoad(this);
        if ( parameters!=null ) { parameters.forEach(d->d.postLoad(this)); }
        if ( addRequestTargets!=null ) { addRequestTargets.forEach(d->d.postLoad(this)); }
        steps.forEach(d->d.postLoad(this));
        if ( valueTemplates!=null ) {
            valueTemplates.forEach(d->d.postLoad(this));
        }
    }
    
    private static final void check(boolean isFailure, Object entity, Supplier<String> msgSupplier) {
        if ( isFailure ) {
            throw new ActionValidationException(msgSupplier.get(), entity);
        }
    }
    
    /**
     * Utility method for checking whether the given value is not blank, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being blank
     * @param entity The object containing the property to be checked
     */
    private static final void checkNotBlank(String property, String value, Object entity) {
        check(StringUtils.isBlank(value), entity, ()->String.format("Action %s property must be specified", property));
    }
    
    /**
     * Utility method for checking whether the given value is not null, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being null
     * @param entity The object containing the property to be checked
     */
    private static final void checkNotNull(String property, Object value, Object entity) {
        check(value==null, entity, ()->String.format("Action %s property must be specified", property));
    }
    
    public static interface IActionIfSupplier {
        SimpleExpression get_if();
    }
    
    /**
     * This class describes action usage header and description.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionUsageDescriptor {
        /** Required usage header */
        private String header;
        /** Required usage description */
        private String description;
        
        public void postLoad(ActionDescriptor action) {
            checkNotBlank("usage header", header, this);
            checkNotBlank("usage description", description, this);
        }
    }
    
    /**
     * This class describes a request target.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionRequestTargetDescriptor {
        /** Required name */
        private String name;
        /** Required base URL */
        private TemplateExpression baseUrl;
        /** Optional headers */
        private Map<String, TemplateExpression> headers;
        // TODO Add support for next page URL producer
        // TODO ? Add proxy support ?
        
        public final void postLoad(ActionDescriptor action) {
            checkNotBlank("request target name", name, this);
            checkNotNull("request target base URL", baseUrl, this);
        }
    }
    
    /**
     * This class describes default values for various action properties.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionDefaultValuesDescriptor {
        /** Default value for {@link ActionStepRequestDescriptor#from} */
        private String requestTarget;
    }
    
    /**
     * This class describes a action parameter.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionParameterDescriptor {
        /** Required parameter name */
        private String name;
        /** Optional comma-separated CLI aliases */
        private String cliAliases;
        /** Required parameter description */
        private String description;
        /** Optional parameter type */
        private String type;
        /** Optional type parameters*/
        private Map<String, TemplateExpression> typeParameters;
        /** Optional template expression defining the default parameter value if not provided by user */
        private TemplateExpression defaultValue;
        /** Boolean indicating whether this parameter is required, default is true */
        private boolean required = true;
        
        public final void postLoad(ActionDescriptor action) {
            checkNotBlank("parameter name", name, this);
            checkNotNull("parameter description", getDescription(), this);
            // TODO Check no duplicate names; ideally ActionRunner should also verify
            //      that option names/aliases don't conflict with command options
            //      like --help/-h, --log-file, ...
        }
        
        public final String[] getCliAliasesArray() {
            if ( cliAliases==null ) { return new String[] {}; }
            return Stream.of(cliAliases).map(String::trim).toArray(String[]::new);
        }
    }
    
    /**
     * This class describes a single action step element, which may contain 
     * requests, progress message, and/or set instructions. This class is 
     * used for both top-level step elements, and step elements in forEach elements. 
     * TODO Potentially, later versions may add support for other step types. Some ideas
     *      for potentially useful steps:
     *      <ul>
     *       <li>if: Execute sub-steps only if condition evaluates to true</li>
     *       <li>forEach: Execute sub-steps for every value in input array</li>
     *       <li>fcli: Run other fcli commands to allow for workflow-oriented templates.
     *           Primary question is what to do with output, i.e., store JSON output
     *           in 'data', ability to output regular command output to console (but
     *           how to avoid interference with ProgressWriter?), ...</li>
     *      </ul>
     *
     * @author Ruud Senden
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepDescriptor implements IActionIfSupplier {
        /** Optional if-expression, executing this step only if condition evaluates to true */
        @JsonProperty("if") private SimpleExpression _if;
        /** Optional requests for this step element */
        private List<ActionStepRequestDescriptor> requests;
        /** Optional progress message template expression for this step element */
        private TemplateExpression progress;
        /** Optional warning message template expression for this step element */
        private TemplateExpression warn;
        /** Optional debug message template expression for this step element */
        private TemplateExpression debug;
        /** Optional exception message template expression for this step element */
        @JsonProperty("throw") private TemplateExpression _throw;
        /** Optional set operations */
        private List<ActionStepSetDescriptor> set;
        /** Optional write operations */
        private List<ActionStepWriteDescriptor> write;
        
        /**
         * This method is invoked by the parent element (which may either be another
         * step element, or the top-level {@link ActionDescriptor} instance).
         * It invokes the postLoad() method on each request descriptor.
         */
        public final void postLoad(ActionDescriptor action) {
            if ( requests!=null ) { requests.forEach(d->d.postLoad(action)); }
            if ( set!=null ) { set.forEach(d->d.postLoad(action)); }
            if ( write!=null ) { write.forEach(d->d.postLoad(action)); }
        }
    }
    
    /**
     * This class describes a forEach element, allowing iteration over the output of
     * the parent element, like the response of a REST request or the contents of a
     * action parameter. 
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepForEachDescriptor implements IActionIfSupplier {
        /** Optional if-expression, executing steps only if condition evaluates to true */
        @JsonProperty("if") private SimpleExpression _if;
        /** Optional break-expression, terminating forEach if condition evaluates to true */
        private SimpleExpression breakIf;
        /** Required name for this step element */
        private String name;
        /** Optional requests for which to embed the response in each forEach node */
        private List<ActionStepRequestDescriptor> embed;
        /** Steps to be repeated for each value */
        @JsonProperty("do") private List<ActionStepDescriptor> _do;
        
        /**
         * This method is invoked by the {@link ActionStepDescriptor#postLoad()}
         * method. It checks that required properties are set, then calls the postLoad() method for
         * each sub-step.
         */
        public final void postLoad(ActionDescriptor action) {
            checkNotBlank("forEach name", name, this);
            checkNotNull("forEach do", _do, this);
            if ( embed!=null ) { embed.forEach(d->d.postLoad(action)); }
            _do.forEach(d->d.postLoad(action));
        }
    }
    
    /**
     * This class describes an operation to explicitly set a data property.
     * Note that data properties for request outputs are set automatically.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepSetDescriptor implements IActionIfSupplier {
        /** Optional if-expression, executing this step only if condition evaluates to true */
        @JsonProperty("if") private SimpleExpression _if;
        /** Required name for this step element */
        private String name;
        /** Value template expression for this step element */
        private TemplateExpression value;
        /** Value template used to generate the value to be set */
        private String valueTemplate;
        /** Operation to be performed when setting the value */
        private ActionStepSetOperation operation;
        
        public void postLoad(ActionDescriptor action) {
            checkNotBlank("set name", name, this);
            if ( value==null && valueTemplate==null ) {
                valueTemplate = name;
            }
            if ( valueTemplate!=null ) {
                check(!action.getValueTemplates().stream().anyMatch(d->d.getName().equals(valueTemplate)), this, 
                        ()->"No value template found with name "+valueTemplate);
            }
        }
    }
    
    // Ideally, this should be an inner class on ActionStepRequestDescriptor, but this causes
    // issues with native images; see https://github.com/formkiq/graalvm-annotations-processor/issues/11
    // TODO: Add other operations like 'merge' for merging two ObjectNodes or ArrayNodes?
    @Reflectable
    public static enum ActionStepSetOperation {
        replace, append;
    }
    
    /**
     * This class describes a 'write' step.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepWriteDescriptor implements IActionIfSupplier {
        /** Optional if-expression, executing this step only if condition evaluates to true */
        @JsonProperty("if") private SimpleExpression _if;
        /** Required template expression defining where to write the data, either stdout, stderr or filename */
        private TemplateExpression to;
        /** Value template expression that generated the contents to be written */
        private TemplateExpression value;
        /** Value template used to generate the value to be set */
        private String valueTemplate;
        
        public void postLoad(ActionDescriptor action) {
            checkNotNull("write to", to, this);
            check(value==null && StringUtils.isBlank(valueTemplate), this, ()->
                "Either value or valueTemplate must be specified on write step");
        }
    }
    
    /**
     * This class describes a REST request.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepRequestDescriptor implements IActionIfSupplier {
        /** Optional if-expression, executing this request only if condition evaluates to true */
        @JsonProperty("if") private SimpleExpression _if;
        /** Required name for this step element */
        private String name;
        /** Optional HTTP method, defaults to 'GET' */
        private HttpMethod method = HttpMethod.GET;
        /** Required template expression defining the request URI from which to get the data */
        private TemplateExpression uri;
        /** Required target to which to send the request; may be specified here or through defaults.requestTarget */
        private String target;
        /** Map defining (non-encoded) request query parameters; parameter values are defined as template expressions */
        private Map<String,TemplateExpression> query;
        /** Optional request body template expression */
        private TemplateExpression body;
        /** Type of request; either 'simple' or 'paged' for now */
        private ActionStepRequestType type = ActionStepRequestType.simple;
        /** Optional progress messages for various stages of request processing */
        private ActionStepRequestPagingProgressDescriptor pagingProgress;
        /** Optional steps to be executed on the response before executing forEach steps */
        private List<ActionStepDescriptor> onResponse;
        /** Optional steps to be executed on request failure; if not declared, an exception will be thrown */
        private List<ActionStepDescriptor> onFail;
        /** Optional forEach block to be repeated for every response element */
        private ActionStepForEachDescriptor forEach;
        
        /**
         * This method is invoked by {@link ActionStepDescriptor#postLoad()}
         * method. It checks that required properties are set.
         */
        protected final void postLoad(ActionDescriptor action) {
            checkNotBlank("request name", name, this);
            checkNotNull("request uri", uri, this);
            if ( StringUtils.isBlank(target) && action.defaults!=null ) {
                target = action.defaults.requestTarget;
            }
            checkNotBlank("request target", target, this);
            if ( pagingProgress!=null ) {
                type = ActionStepRequestType.paged;
            }
            if ( forEach!=null ) {
                forEach.postLoad(action);
            }
        }
    }
    
    // Ideally, this should be an inner class on ActionStepRequestDescriptor, but this causes
    // issues with native images; see https://github.com/formkiq/graalvm-annotations-processor/issues/11
    @Reflectable
    public static enum ActionStepRequestType {
        simple, paged
    }
    
    // Ideally, this should be an inner class on ActionStepRequestDescriptor, but this causes
    // issues with native images; see https://github.com/formkiq/graalvm-annotations-processor/issues/11
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepRequestPagingProgressDescriptor {
        private TemplateExpression prePageLoad;
        private TemplateExpression postPageLoad;
        private TemplateExpression postPageProcess;
    }
    
    /**
     * This class describes an output, which can be either a top-level output
     * or partial output.
     */
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionValueTemplateDescriptor {
        /** Required name for this output */
        private String name;
        /** Output contents in JSON format, where each text node is assumed to be a template expression */
        private JsonNode contents;
        /** Cached mapping from text node property path to corresponding TemplateExpression instance */  
        private final Map<String, TemplateExpression> valueExpressions = new LinkedHashMap<>();
        
        /**
         * This method checks whether required name and contents are not blank or null, then
         * walks the given contents to parse each text node as a {@link TemplateExpression},
         * caching the resulting {@link TemplateExpression} instance in the {@link #valueExpressions}
         * map, throwing an exception if the text node cannot be parsed as a {@link TemplateExpression}.
         */
        public final void postLoad(ActionDescriptor action) {
            checkNotBlank("(partial) output name", name, this);
            checkNotNull("(partial) output contents", contents, this);
            new ContentsWalker().walk(contents);
        }
        
        private final class ContentsWalker extends AbstractJsonNodeWalker<Void, Void> {
            @Override
            protected Void getResult() { return null; }
            @Override
            protected void walkValue(Void state, String path, JsonNode parent, ValueNode node) {
                if ( node instanceof TextNode ) {
                    var expr = node.asText();
                    try {
                        valueExpressions.put(path, SpelHelper.parseTemplateExpression(expr));
                    } catch (ParseException e) {
                        throw new ActionValidationException(String.format("Error parsing template expression '%s'", expr), ActionValueTemplateDescriptor.this, e);
                    }
                }
                super.walkValue(state, path, parent, node);
            }
        }
    }
    
    /**
     * Exception class used for action validation errors.
     */
    public static final class ActionValidationException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public ActionValidationException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public ActionValidationException(String message, Object actionElement, Throwable cause) {
            this(getMessageWithEntity(message, actionElement), cause);
        }

        public ActionValidationException(String message) {
            super(message);
        }
        
        public ActionValidationException(String message, Object actionElement) {
            this(getMessageWithEntity(message, actionElement));
        }

        private static final String getMessageWithEntity(String message, Object actionElement) {
            return String.format("%s (entity: %s)", message, actionElement.toString());
        }  
    }
}
