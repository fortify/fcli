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
package com.fortify.cli.common.cli.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.mcp.MCPInclude;
import com.fortify.cli.common.output.cli.cmd.IOutputHelperSupplier;
import com.fortify.cli.common.output.cli.mixin.QueryOptionMixin;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.ReflectionHelper;

import lombok.Setter;
import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.Messages;

public class FcliCommandSpecHelper {
    @Setter // Injected by DefaultFortifyCLIRunner
    private static CommandLine rootCommandLine;
    
    public static final CommandLine getRootCommandLine() {
        if ( rootCommandLine==null ) {
            throw new FcliBugException("Root command line hasn't been configured upon fcli initialization");
        }
        return rootCommandLine;
    }
    
    public static final CommandSpec getRootCommandSpec() {
        return getRootCommandLine().getCommandSpec().root();
    }
    
    public static final CommandSpec getCommandSpec(String cmd) {
        var currentSpec = getRootCommandSpec();
        if ( StringUtils.isBlank(cmd) ) { return currentSpec; }
        for ( var elt : cmd.replaceAll("^fcli\\s+", "").split("\\s+") ) {
            var cl = currentSpec.subcommands().get(elt);
            if ( cl==null ) { return null; }
            currentSpec = cl.getCommandSpec();
        }
        return currentSpec;
    }
    
    public static final Stream<CommandSpec> rootCommandTreeStream() {
        return commandTreeStream(getRootCommandSpec());
    } 
    
    public static final Stream<CommandSpec> commandTreeStream(CommandSpec commandSpec) {
        if (commandSpec == null) { return Stream.empty(); }
        var subcommands = commandSpec.subcommands();
        Stream<CommandSpec> subcommandStreams = subcommands == null || subcommands.isEmpty()
                ? Stream.empty()
                : subcommands.values().stream()
                    .map(CommandLine::getCommandSpec)
                    .flatMap(FcliCommandSpecHelper::commandTreeStream);
        return Stream.concat(Stream.of(commandSpec), subcommandStreams).distinct();
    }
    
    public static final <T extends Annotation> T findAnnotation(CommandSpec commandSpec, Class<T> annotationType) {
        T annotation = null;
        while ( commandSpec!=null && annotation==null ) {
            Object cmd = commandSpec.userObject();
            annotation = cmd==null ? null : cmd.getClass().getAnnotation(annotationType);
            commandSpec = commandSpec.parent();
        }
        return annotation;
    }
    
    public static final <T extends Annotation> T getAnnotation(CommandSpec cmdSpec, Class<T> annotationType) {
        var userObject = cmdSpec==null ? null : cmdSpec.userObject();
        if ( userObject!=null ) {
            return userObject.getClass().getAnnotation(annotationType);
        }
        return null;
    }
    
    public static final <T extends Annotation> T getAnnotation(ArgSpec argSpec, Class<T> annotationType) {
        var userObject = argSpec==null ? null : argSpec.userObject();
        if ( userObject!=null && userObject instanceof AccessibleObject ) {
            return ((AccessibleObject) userObject).getAnnotation(annotationType);
        }
        return null;
    }
    
    public static final String getCommandGroup(CommandSpec cmdSpec) {
        var annotation = getAnnotation(cmdSpec, CommandGroup.class);
        return annotation==null ? null : annotation.value();
    }
    
    public static final String getMessageString(CommandSpec commandSpec, String keySuffix, Object... args) {
        var group = getCommandGroup(commandSpec);
        Messages messages = getMessages(commandSpec);
        String value = null;
        while ( commandSpec!=null && value==null ) {
            String pfx = commandSpec.qualifiedName(".")+".";
            value = getMessageString(messages, pfx, group, keySuffix);
            commandSpec = commandSpec.parent();
        }
        // If value is still null, try without any prefix
        value = value!=null ? value : getMessageString(messages, "", group, keySuffix);
        return formatMessage(value, args);
    }
        
    private static final String getMessageString(Messages messages, String pfx, String group, String sfx) {
        String value = null;
        if ( StringUtils.isNotBlank(group) ) {
            value = messages.getString(pfx+group+"."+sfx, null);
        }
        return value!=null ? value : messages.getString(pfx+sfx, null);
    }
    
    private static final String formatMessage(String msg, Object... args) {
        return msg==null || args==null || args.length==0 ? msg : String.format(msg, args);
    }
    
    public static final String getRequiredMessageString(CommandSpec commandSpec, String keySuffix, Object... args) {
        String result = getMessageString(commandSpec, keySuffix, args);
        if ( StringUtils.isBlank(result) ) {
            throw new FcliBugException("No resource bundle entry found for required key suffix: "+keySuffix);
        }
        return result;
    }
    
    /**
     * @param commandSpec {@link CommandSpec} instance for looking up a {@link ResourceBundle}
     * @return {@link Messages} instance for the given {@link CommandSpec}, 
     *         or null if {@link CommandSpec} doesn't have a {@link ResourceBundle}
     */
    public static final Messages getMessages(CommandSpec commandSpec) {
        ResourceBundle resourceBundle = commandSpec.resourceBundle();
        return resourceBundle==null ? null : new Messages(commandSpec, resourceBundle);
    }
    
    public static final boolean isHiddenSelf(CommandSpec spec) {
        return spec.usageMessage().hidden();
    }
    
    public static final boolean hasHiddenParent(CommandSpec spec) {
        var parent = spec.parent();
        if ( parent==null ) { return false; }
        if ( parent.usageMessage().hidden() ) { return true; }
        return hasHiddenParent(parent);
    }
    
    public static final boolean isHiddenSelfOrParent(CommandSpec spec) {
        return isHiddenSelf(spec) || hasHiddenParent(spec);
    }
    
    public static final boolean isMcpIgnored(CommandSpec spec) {
        if ( spec.userObject().getClass().isAnnotationPresent(MCPInclude.class) ) { return false; }
        return findAnnotation(spec, MCPExclude.class)!=null
                || isMcpIgnoredCommandName(spec)
                || isHiddenSelfOrParent(spec)
                || !isRunnable(spec)
                || hasRequiredSensitiveArgs(spec)
                || isDeprecated(spec);
    }
    
    private static final boolean isDeprecated(CommandSpec spec) {
        var header = spec.usageMessage().header();
        return header==null || Stream.of(header).anyMatch(h->h.indexOf("DEPRECATED")>=0);
    }

    private static boolean isMcpIgnoredCommandName(CommandSpec spec) {
        // Using a single regular expression, given a string like "fcli module entity action", ignore if:
        // action starts with reset, revoke, delete, clear
        // entity is access-control or action
        // entity is rest and action is call
        var qualifiedName = spec.qualifiedName(" ");
        return Pattern.compile(
                "(^\\S+ \\S+ \\S+ (reset|revoke|delete|clear|purge|uninstall|cancel|disable|logout|update)\\S*$)"+
                "|(^\\S+ \\S+ (access-control|action) \\S+$)"+
                "|(^\\S+ \\S+ rest call$)")
                .matcher(qualifiedName).matches();
    }

    public static final boolean isRunnable(CommandSpec spec) {
        var userObject = userObject(spec);
        return userObject!=null && (userObject instanceof Runnable || userObject instanceof Callable<?>);
    }
    
    public static final Object userObject(CommandSpec spec) {
        return spec==null ? null : spec.userObject();
    }
    
    public static final boolean canCollectRecords(CommandSpec spec) {
        return JavaHelper.as(spec.userObject(), IOutputHelperSupplier.class).isPresent();
    }
    
    public static final boolean hasRequiredSensitiveArgs(CommandSpec cs) {
        return Stream.concat(cs.options().stream(), cs.positionalParameters().stream())
                .anyMatch(as->isRequiredSensitiveArg(as));
    }

    private static final boolean isRequiredSensitiveArg(ArgSpec as) {
    return as.required() && isSensitive(as);
    }

    public static final boolean isSensitive(ArgSpec as) {
        return (as.interactive() && !as.echo()) 
            || ReflectionHelper.getAnnotationValue(as.userObject(), MaskValue.class, MaskValue::sensitivity, ()->null)==LogSensitivityLevel.high;
    }
    
    /**
     * Recursively returns a Stream of all mixins (including nested ones) for the given CommandSpec.
     * The returned stream includes only mixin CommandSpec instances, not the root CommandSpec itself.
     */
    public static Stream<CommandSpec> getAllMixinsStream(CommandSpec commandSpec) {
        if (commandSpec == null) return Stream.empty();
        return commandSpec.mixins().values().stream()
            .flatMap(mixin -> Stream.concat(
                Stream.of(mixin),
                getAllMixinsStream(mixin)
            ));
    }
        
    public static final Optional<QueryExpression> getQueryExpression(CommandSpec commandSpec) {
        return getQueryOptionMixin(commandSpec)
                .map(s -> s.getQueryExpression())
                .filter(Objects::nonNull);
    }

    public static final Optional<QueryOptionMixin> getQueryOptionMixin(CommandSpec commandSpec) {
        return getAllMixinsStream(commandSpec)
                .map(m -> m.userObject())
                .filter(o -> o instanceof QueryOptionMixin)
                .findFirst()
                .map(o -> (QueryOptionMixin)o);
    }
}