package com.fortify.cli;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fortify.cli.app.FCLIRootCommands;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.StringUtils;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

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
        checkCommandNamingConvention(results, spec);
        checkMaxCommandDepth(results, spec);
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
        // TODO Any tests specific for leaf commands?
    }
    
    private void checkOptions(Results results, CommandSpec cmdSpec) {
        checkStandardOptions(results, cmdSpec);
        cmdSpec.options().forEach(optionSpec->checkOptionSpec(results, cmdSpec, optionSpec));
    }

    private void checkStandardOptions(Results results, CommandSpec spec) {
        var optionNames = spec.optionsMap().keySet();
        var expectedOptionNames = Arrays.asList("-h", "--help", "--log-level", "--log-file", "--env-prefix");
        if ( !optionNames.containsAll(expectedOptionNames) ) {
            results.add(TestType.CMD_STD_OPTS, Level.ERROR, spec, "Missing one or more standard option names: "+expectedOptionNames);
        }
    }
    
    private void checkOptionSpec(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        checkOptionNames(results, cmdSpec, optionSpec);
        checkOptionArity(results, cmdSpec, optionSpec);
    }
    
    private void checkOptionNames(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        Stream.of(optionSpec.names()).filter(this::isInvalidOptionName).forEach(
                name->results.add(TestType.OPT_NAME, Level.ERROR, cmdSpec, optionSpec, "Invalid option name: "+name));
        if ( optionSpec.isMultiValue() ) {
            // TODO For now we only output warnings for FoD; update once FoD commands have been fixed
            Level resultType = isFoD(cmdSpec) ? Level.WARN : Level.ERROR;
            Stream.of(optionSpec.names()).filter(n->n.startsWith("--") && !n.endsWith("s")).forEach(
                name->results.add(TestType.MULTI_OPT_PLURAL_NAME, resultType, cmdSpec, optionSpec, "Multi-value option should use plural option name: "+name));
            if ( StringUtils.isBlank(optionSpec.splitRegex()) ) {
                results.add(TestType.MULTI_OPT_SPLIT, resultType, cmdSpec, optionSpec, "Multi-value option should define a split expression");
            }
        }
    }
    
    private void checkOptionArity(Results results, CommandSpec cmdSpec, OptionSpec optionSpec) {
        var arity = optionSpec.arity();
        // TODO For now we only output warnings for FoD; update once FoD commands have been fixed
        Level resultType = isFoD(cmdSpec) ? Level.WARN : Level.ERROR;
        if ( arity.isVariable() ) {
            results.add(TestType.OPT_ARITY_VARIABLE, resultType, cmdSpec, optionSpec, "Variable arity not allowed: "+arity.originalValue());
        } else if ( !arity.isUnspecified() ) {
            if ( optionSpec.type().isAssignableFrom(Boolean.class) || optionSpec.type().isAssignableFrom(boolean.class) ) {
                if ( arity.min()!=arity.max() || (arity.min()!=0 && arity.min()!=1) ) {
                    results.add(TestType.OPT_ARITY_BOOL, resultType, cmdSpec, optionSpec, "Arity for boolean options must be either 0 (flags) or 1 (require true/false value)");
                }
            } else if ( optionSpec.interactive() ) {
                if ( arity.min()!=0 || arity.max()!=1 ) {
                    results.add(TestType.OPT_ARITY_INTERACTIVE, resultType, cmdSpec, optionSpec, "Arity for interactive options must be 0..1");
                }
            } else {
                results.add(TestType.OPT_ARITY_PRESENT, resultType, cmdSpec, optionSpec, "Arity may only be specified on boolean or interactive options");
            }
        }
    }
    
    private boolean isInvalidOptionName(String s) {
        // Check for either single-letter option (which may be upper/lower-case or number)
        // or long option; long options must be at least two characters after double dash
        return !s.matches("-[a-zA-Z0-9]|--(?!-)[a-z0-9-]+[^-]");
    }
    
    private void checkCommandNamingConvention(Results results, CommandSpec spec) {
        if ( isInvalidCommandName(spec.name()) ) { 
            results.add(TestType.CMD_NAME, Level.ERROR, spec, "Invalid command name: "+spec.name()); 
        }
        Stream.of(spec.aliases()).filter(this::isInvalidCommandName).forEach(
                name->results.add(TestType.CMD_NAME, Level.ERROR, spec, "Invalid alias name: "+name));
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
    
    private final boolean isFoD(CommandSpec cmdSpec) {
        return cmdSpec.qualifiedName().startsWith("fcli fod");
    }
    
    private static class Results {
        private final Map<Level, Set<String>> results = new HashMap<>();
        
        private void add(TestType type, Level level, CommandSpec cmdSpec, OptionSpec optionSpec, String msg) {
            if ( isDisabled(type, cmdSpec, optionSpec) ) {
                level = Level.INFO;
                msg = type.name()+ " test disabled";
            }
            add(level, cmdSpec.qualifiedName()+" "+optionSpec.longestName()+": "+msg);
        }
        
        private void add(TestType type, Level level, CommandSpec cmdSpec, String msg) {
            if ( isDisabled(type, cmdSpec, null) ) {
                level = Level.INFO;
                msg = type.name()+ " test disabled";
            }
            add(level, cmdSpec.qualifiedName()+": "+msg);
        }
        
        private void add(Level level, String msg) {
            var set = results.computeIfAbsent(level, t->new TreeSet<String>());
            set.add(level.name().toUpperCase()+": "+msg);
            results.put(level, set);
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
        
        private boolean isDisabled(TestType type, CommandSpec cmdSpec, OptionSpec optionSpec) {
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
