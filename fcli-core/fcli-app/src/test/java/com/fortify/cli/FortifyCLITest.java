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
package com.fortify.cli;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.StringUtils;

import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Spec;

public class FortifyCLITest {
    /**
     * This test performs multiple checks on the fcli command tree.
     * We iterate the command tree only once for performance reasons,
     * collecting all results into a single failure if applicable.
     * @throws Exception
     */
    @Test
    public void testCommands() throws Exception {
        Results results = new Results();
        checkCommand(results, new CommandLine(FCLIRootCommands.class));
        results.process();
    }

    private void checkCommands(Results results, Map<String, CommandLine> commands) {
        if ( commands!=null && !commands.isEmpty() ) {
            commands.values().stream().forEach(c->checkCommand(results,c));
        }
    }

    private void checkCommand(Results results, CommandLine cl) {
        CommandSpec spec = cl.getCommandSpec();
        checkOptions(results, spec);
        checkParameters(results, spec);
        checkCommandNamingConvention(results, spec);
        checkUsageHeader(results, spec);
        checkMaxCommandDepth(results, spec);
        checkMixins(results, spec, spec.mixins());
        Map<String, CommandLine> subcommands = spec.subcommands();
        if ( subcommands==null || subcommands.isEmpty() ) {
            checkLeafCommand(results, spec);
        } else {
            checkContainerCommand(results, spec);
        }
    }

    private void checkContainerCommand(Results results, CommandSpec spec) {
        // TODO Any tests specific for container commands?
        checkCommands(results, spec.subcommands());
    }

    private void checkLeafCommand(Results results, CommandSpec spec) {
        checkDefaultTableOptionsPresent(results, spec);
    }
    
    private void checkDefaultTableOptionsPresent(Results results, CommandSpec spec) {
        if ( spec.mixins().containsKey("outputHelper") ) {
            var tableOptions = new CommandSpecMessageResolver(spec).getMessageString("output.table.options");
            if ( StringUtils.isBlank(tableOptions) ) {
                results.add(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT, Level.ERROR, spec, getBundleName(spec)+": No *.output.table.options defined to specify default table output columns");
            }
        }
    }

    private void checkOptions(Results results, CommandSpec cmdSpec) {
        checkStandardOptions(results, cmdSpec);
        cmdSpec.options().forEach(optionSpec->checkOptionSpec(results, cmdSpec, optionSpec));
    }

    private void checkStandardOptions(Results results, CommandSpec spec) {
        var optionNames = spec.optionsMap().keySet();
        var expectedOptionNames = spec.userObject() instanceof Runnable
                ? Arrays.asList("-h", "--help", "--log-level", "--log-file", "--env-prefix")
                : Arrays.asList("-h", "--help");
        if ( !optionNames.containsAll(expectedOptionNames) ) {
            results.add(TestType.CMD_STD_OPTS, Level.ERROR, spec, "Missing one or more standard option names: "+expectedOptionNames);
        }
    }
    
    private void checkOptionSpec(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        checkOptionNames(results, cmdSpec, optionSpec);
        checkMultiValueOption(results, cmdSpec, optionSpec);
        checkOptionArity(results, cmdSpec, optionSpec);
        checkOptionDescription(results, cmdSpec, optionSpec);
    }
    
    private void checkOptionNames(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        var names = optionSpec.names();
        Stream.of(names).filter(this::isInvalidOptionFormat).forEach(
                name->results.add(TestType.OPT_NAME_FORMAT, Level.ERROR, cmdSpec, optionSpec, "Invalid option format: "+name));
        if ( Stream.of(names).filter(this::isShortOption).count() > 1 ) {
            results.add(TestType.OPT_SHORT_NAME_COUNT, Level.ERROR, cmdSpec, optionSpec, "Option must have at most 1 short name");
        }
        if ( Stream.of(names).filter(this::isLongOption).count() < 1 ) {
            results.add(TestType.OPT_LONG_NAME_COUNT, Level.ERROR, cmdSpec, optionSpec, "Option must have at least 1 long name");
        }
        Stream.of(names).filter(this::isShortOption).filter(this::isInvalidShortOptionName).forEach(
                name->results.add(TestType.OPT_SHORT_NAME, Level.ERROR, cmdSpec, optionSpec, "Invalid short option name: "+name));
        Stream.of(names).filter(this::isLongOption).filter(this::isInvalidLongOptionName).forEach(
                name->results.add(TestType.OPT_LONG_NAME, Level.ERROR, cmdSpec, optionSpec, "Invalid long option name: "+name));        
    }
    
    private void checkMultiValueOption(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        if ( optionSpec.isMultiValue() ) {
            Stream.of(optionSpec.names()).filter(n->n.startsWith("--") && !n.endsWith("s") && !n.contains("any")).forEach(
                name->results.add(TestType.MULTI_OPT_PLURAL_NAME, Level.ERROR, cmdSpec, optionSpec, "Multi-value option should use plural option name: "+name));
            if ( StringUtils.isBlank(optionSpec.splitRegex()) ) {
                results.add(TestType.MULTI_OPT_SPLIT, Level.ERROR, cmdSpec, optionSpec, "Multi-value option should define a split expression");
            }
        }
    }
    
    private void checkOptionArity(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        var arity = optionSpec.arity();
        if ( arity.isVariable() ) {
            results.add(TestType.OPT_ARITY_VARIABLE, Level.ERROR, cmdSpec, optionSpec, "Variable arity not allowed: "+arity.originalValue());
        } else if ( !arity.isUnspecified() ) {
            if ( optionSpec.type().isAssignableFrom(Boolean.class) || optionSpec.type().isAssignableFrom(boolean.class) ) {
                if ( arity.min()!=arity.max() || (arity.min()!=0 && arity.min()!=1) ) {
                    results.add(TestType.OPT_ARITY_BOOL, Level.ERROR, cmdSpec, optionSpec, "Arity for boolean options must be either 0 (flags) or 1 (require true/false value)");
                }
            } else if ( optionSpec.interactive() ) {
                if ( arity.min()!=0 || arity.max()!=1 ) {
                    results.add(TestType.OPT_ARITY_INTERACTIVE, Level.ERROR, cmdSpec, optionSpec, "Arity for interactive options must be 0..1");
                }
            } else {
                results.add(TestType.OPT_ARITY_PRESENT, Level.ERROR, cmdSpec, optionSpec, "Arity may only be specified on boolean or interactive options");
            }
        }
    }
    
    private void checkOptionDescription(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        var descriptionArray = optionSpec.description();
        if ( descriptionArray.length==0 || Stream.of(descriptionArray).allMatch(StringUtils::isBlank) ) {
            String descriptionKey = StringUtils.isBlank(optionSpec.descriptionKey()) ? "<default>" : optionSpec.descriptionKey();
            String cmd = cmdSpec.qualifiedName();
            results.add(TestType.OPT_EMPTY_DESCRIPTION, Level.ERROR, cmdSpec, optionSpec, String.format("%s: Option has an empty description (%s, descriptionKey=%s)", getBundleName(cmdSpec), cmd, descriptionKey));
        }
    }

    /**
     * @param cmdSpec
     * @return
     */
    private String getBundleName(CommandSpec cmdSpec) {
        return StringUtils.substringAfterLast(cmdSpec.resourceBundleBaseName(), ".");
    }
    
    private boolean isInvalidOptionFormat(String s) {
        return !isLongOption(s) && !isShortOption(s);
    }
    
    private boolean isLongOption(String s) {
        // Long options must start with double dash, followed by any number of
        // characters
        return s.startsWith("--");
    }
    
    private boolean isShortOption(String s) {
        // Short options must start with single dash, followed by single other character
        return s.startsWith("-") && s.length()==2;
    }
    
    private boolean isInvalidShortOptionName(String s) {
        // Short options must start with a single dash, followed by a single
        // lower-case letter or number
        return !s.matches("-[a-z0-9]");
    }
    
    private boolean isInvalidLongOptionName(String s) {
        // Long options must be at least two characters after double dash,
        // may contain only lower-case letters and numbers, optionally 
        // separated by dashes. but not ending with a dash 
        return !s.matches("--(?!-)[a-z0-9-]+[^-]");
    }
    
    private void checkParameters(Results results, CommandSpec cmdSpec) {
        cmdSpec.positionalParameters().forEach(paramSpec->checkParameterSpec(results, cmdSpec, paramSpec));
    }
    
    private void checkParameterSpec(Results results, CommandSpec cmdSpec, PositionalParamSpec paramSpec) {
        checkParameterDescription(results, cmdSpec, paramSpec);
    }
    
    private void checkParameterDescription(Results results, CommandSpec cmdSpec, PositionalParamSpec paramSpec) {
        var descriptionArray = paramSpec.description();
        if ( descriptionArray.length==0 || Stream.of(descriptionArray).allMatch(StringUtils::isBlank) ) {
            String descriptionKey = StringUtils.isBlank(paramSpec.descriptionKey()) ? "<default>" : paramSpec.descriptionKey();
            results.add(TestType.PARAM_EMPTY_DESCRIPTION, Level.ERROR, cmdSpec, paramSpec, String.format("%s: Positional parameter has an empty description (descriptionKey=%s)", getBundleName(cmdSpec), descriptionKey));
        }
    }

    private void checkUsageHeader(Results results, CommandSpec spec) {
        if ( !spec.equals(spec.root()) ) {
            var rootHeader = spec.root().usageMessage().header();
            var cmdHeader = spec.usageMessage().header();
            if ( cmdHeader==null 
                    || Stream.of(cmdHeader).allMatch(StringUtils::isBlank) 
                    || Arrays.equals(rootHeader, cmdHeader) ) {
                results.add(TestType.CMD_USAGE_HEADER, Level.ERROR, spec, getBundleName(spec)+": Command doesn't define proper usage header: "+Arrays.asList(cmdHeader));
            }
        }
    }
    
    private void checkCommandNamingConvention(Results results, CommandSpec spec) {
        if ( isInvalidCommandName(spec.name()) ) { 
            results.add(TestType.CMD_NAME, Level.ERROR, spec, "Invalid command name: "+spec.name()); 
        }
        Stream.of(spec.aliases()).filter(this::isInvalidCommandName).forEach(
                name->results.add(TestType.CMD_NAME, Level.ERROR, spec, "Invalid alias name: "+name));
    }
    
    private void checkMixins(Results results, CommandSpec cmdSpec, Map<String, CommandSpec> mixins) {
        if ( mixins!=null  ) {
            mixins.values().forEach(mixin->checkMixin(results, cmdSpec, mixin));
        }
    }
    
    private void checkMixin(Results results, CommandSpec cmdSpec, CommandSpec mixinSpec) {
        checkMixins(results, cmdSpec, mixinSpec.mixins());
        checkMixeeAnnotationPresent(results, cmdSpec, mixinSpec);
    }

    private void checkMixeeAnnotationPresent(Results results, CommandSpec cmdSpec, CommandSpec mixinSpec) {
        Object mixin = mixinSpec.userObject();
        if ( mixin!=null ) {
            checkMixeeAnnotation(results, cmdSpec, mixinSpec, mixin.getClass().getDeclaredFields());
            checkMixeeAnnotation(results, cmdSpec, mixinSpec, mixin.getClass().getMethods());
        }
    }

    private void checkMixeeAnnotation(Results results, CommandSpec cmdSpec, CommandSpec mixinSpec, AccessibleObject[] accessibleObjects) {
        for ( var accessibleObject : accessibleObjects ) {
            var annotation = accessibleObject.getAnnotation(Spec.class);
            if ( annotation!=null ) {
                results.add(TestType.INJECT_MIXEE, Level.ERROR, cmdSpec, "Mixin class must use CommandHelperMixin to access CommandSpec: "+mixinSpec.userObject().getClass().getName());
            }
        }
    }
    
    private boolean isInvalidCommandName(String s) {
        // Check that command is kebab-case, not starting or ending with dash
        // Command name must be at least two characters
        return !s.matches("(?!-)[a-z0-9-]+[^-]$");
    }
    
    private void checkMaxCommandDepth(Results results, CommandSpec spec) {
        // Check command depth doesn't exceed the maximum depth
        final int maxDepth = 4;
        if ( spec.qualifiedName().chars().filter(ch -> ch == ' ').count() > maxDepth-1 ) {
            results.add(TestType.CMD_DEPTH, Level.ERROR, spec, "Command depth > "+maxDepth);
        }
    }
    
    private static class Results {
        private final Map<Level, Set<String>> results = new HashMap<>();
        
        private void add(TestType type, Level level, CommandSpec cmdSpec, ArgSpec argSpec, String msg) {
            if ( isDisabled(type, cmdSpec, argSpec) ) {
                level = Level.INFO;
                msg = type.name()+ " test disabled";
            }
            var argUserObject = argSpec.userObject();
            String name = null;
            Class<?> clazz;
            if ( argUserObject instanceof Parameter ) {
                var param = (Parameter)argUserObject;
                clazz = param.getDeclaringExecutable().getDeclaringClass();
                name = param.getDeclaringExecutable().getName() + "() - " + param.getName();
            } else {
                var member = (Member)argUserObject;
                clazz = member.getDeclaringClass();
                name = member.getName();
            }
            add(level, getClazzName(clazz) + "::" + name+": "+msg);
        }

        private void add(TestType type, Level level, CommandSpec cmdSpec, String msg) {
            if ( isDisabled(type, cmdSpec, null) ) {
                level = Level.INFO;
                msg = type.name()+ " test disabled";
            }
            add(level, getClazzName(cmdSpec.userObject().getClass())+ ": "+msg);
        }
        
        private void add(Level level, String msg) {
            var set = results.computeIfAbsent(level, t->new TreeSet<String>());
            set.add(level.name().toUpperCase()+": "+msg);
            results.put(level, set);
        }
        
        private String getClazzName(Class<?> clazz) {
            var clazzName = clazz.getSimpleName();
            while ( clazz.getDeclaringClass()!=null ) {
                clazz = clazz.getDeclaringClass();
                clazzName = clazz.getSimpleName() + "." + clazzName;
            }
            return clazzName;
        }
        
        private void process() {
            String infoString = getResultsAsString(Level.INFO);
            String warnString = getResultsAsString(Level.WARN);
            String errString = getResultsAsString(Level.ERROR);
            if ( infoString!=null ) {
                System.err.println(infoString);
            }
            if ( warnString!=null ) {
                System.err.println(warnString);
            }
            if ( errString!=null ) {
                System.err.println(errString);
                Assertions.fail("\n"+errString);
            }
        }
        
        private boolean isDisabled(TestType type, CommandSpec cmdSpec, ArgSpec optionSpec) {
            return isDisabled(type, PicocliSpecHelper.getAnnotation(cmdSpec, DisableTest.class))
                    || isDisabled(type, PicocliSpecHelper.getAnnotation(optionSpec, DisableTest.class));
        }
        
        private boolean isDisabled(TestType type, DisableTest annotation) {
            return annotation!=null && Arrays.asList(annotation.value()).contains(type);
        }
        
        private String getResultsAsString(Level level) {
            var resultsForType = results.get(level);
            if ( resultsForType!=null && !resultsForType.isEmpty() ) {
                return resultsForType.stream().collect(Collectors.joining("\n"));
            }
            return null;
        }
    }
    
    private static enum Level {
        INFO, WARN, ERROR
    }
    
    public static void main(String[] args) throws Exception {
        new FortifyCLITest().testCommands();
    }
    
}
