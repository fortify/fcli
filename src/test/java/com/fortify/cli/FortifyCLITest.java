package com.fortify.cli;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fortify.cli.app.FCLIRootCommands;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class FortifyCLITest {
    /**
     * This test performs multiple checks on the fcli command tree.
     * We iterate the command tree only once for performance reasons,
     * collecting all errors into a single failure if applicable.
     * @throws Exception
     */
    @Test
    public void testCommands() throws Exception {
        Set<String> errors = new HashSet<>();
        checkCommand(errors, new CommandLine(FCLIRootCommands.class));
        if ( !errors.isEmpty() ) {
            String msg = errors.stream().collect(Collectors.joining("\n"));
            System.err.println(msg);
            Assertions.fail("\n"+msg);
        }
    }

    private void checkCommands(Set<String> errors, Map<String, CommandLine> commands) {
        if ( commands!=null && !commands.isEmpty() ) {
            commands.values().stream().forEach(c->checkCommand(errors,c));
        }
    }

    private void checkCommand(Set<String> errors, CommandLine cl) {
        CommandSpec spec = cl.getCommandSpec();
        checkStandardOptions(errors, spec);
        checkOptionsNamingConvention(errors, spec);
        checkCommandNamingConvention(errors, spec);
        checkMaxCommandDepth(errors, spec);
        Map<String, CommandLine> subcommands = spec.subcommands();
        if ( subcommands==null || subcommands.isEmpty() ) {
            checkLeafCommand(errors, spec);
        } else {
            checkContainerCommand(errors, spec);
        }
    }

    private void checkContainerCommand(Set<String> errors, CommandSpec spec) {
        // TODO Any tests specific for container commands?
        checkCommands(errors, spec.subcommands());
    }

    private void checkLeafCommand(Set<String> errors, CommandSpec spec) {
        // TODO Any tests specific for leaf commands?
    }
    
    private void checkStandardOptions(Set<String> errors, CommandSpec spec) {
        Set<String> optionNames = spec.optionsMap().keySet();
        List<String> expectedOptionNames = Arrays.asList("-h", "--help", "--log-level", "--log-file", "--env-prefix");
        if ( !optionNames.containsAll(expectedOptionNames) ) {
            addError(errors, spec, "Missing one or more standard option names: "+expectedOptionNames);
        }
    }
    
    private void checkOptionsNamingConvention(Set<String> errors, CommandSpec spec) {
        Set<String> optionNames = spec.optionsMap().keySet();
        optionNames.stream().filter(this::isInvalidOptionName).forEach(
                name->addError(errors, spec, "Invalid option name: "+name));
    }
    
    private boolean isInvalidOptionName(String s) {
        // Check for either single-letter option (which may be upper/lower-case or number)
        // or long option; long options must be at least two characters after double dash
        return !s.matches("-[a-zA-Z0-9]|--(?!-)[a-z0-9-]+[^-]");
    }
    
    private void checkCommandNamingConvention(Set<String> errors, CommandSpec spec) {
        if ( isInvalidCommandName(spec.name()) ) { addError(errors, spec, "Invalid command name: "+spec.name()); }
        Stream.of(spec.aliases()).filter(this::isInvalidCommandName).forEach(
                name->addError(errors, spec, "Invalid alias name: "+name));
    }
    
    private boolean isInvalidCommandName(String s) {
        // Check that command is kebab-case, not starting or ending with dash
        // Command name must be at least two characters
        return !s.matches("(?!-)[a-z0-9-]+[^-]$");
    }
    
    private void checkMaxCommandDepth(Set<String> errors, CommandSpec spec) {
        // Check command depth doesn't exceed the maximum depth
        final int maxDepth = 4;
        if ( spec.qualifiedName().chars().filter(ch -> ch == ' ').count() > maxDepth-1 ) {
            addError(errors, spec, "Command depth > "+maxDepth);
        }
    }

    private void addError(Set<String> errors, CommandSpec spec, String msg) {
        addError(errors, spec.qualifiedName()+": "+msg); 
    }
    
    private void addError(Set<String> errors, String msg) {
        errors.add(msg); 
    }
    
}
