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

import java.io.File;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;

import picocli.CommandLine;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

public class CompletionCandidatesTest {
    // Ignore any fields that end with any of these strings. Be careful not to ignore
    // too much.
    private static final String[] ignoreFieldEndsWith = {
        "name", "id", "user", "users", "userGroups", "email", "phoneNumber", "password", "tenant", 
        "token", "tokens", "secret", "secrets", "description", "message", "owner", "date", "period", 
        "url", "uri", "applications", 
    };
    // Map of <class>::<field> or <field> to alternative messages, allowing to suppress 
    // (empty string) or provide an alternative message after having reviewed the output 
    // of a previous run of this test.
    private static Map<String, String> fieldAltMessagesMap = createFieldAltMessageMap();
    private static final Map<String, String> createFieldAltMessageMap() {
        var altMessages = new String[][]{
            {"delimiter", ""},
            {"envPrefix", ""},
            {"variableStoreConfig", ""},
            {"queryExpression", ""},
            {"logFile", "Should change to File or Path"},
            {"outputFile", "Should change to File or Path"},
            {"trustStorePath", "Should change to File or Path"},
            {"installDir", "Should change to File or Path"},
            {"answerFile", "Should change to File or Path"},
            {"templatePath", "Should change to File or Path"},
            {"destination", "Should change to File or Path"},
            {"seedBundle", "Should change to File or Path"},
            {"attributes", ""},
            {"filtersParam", ""},
            {"microservices", ""},
            {"releaseMicroservice", ""},
            {"scanIds", ""},
            {"chunkSize", ""},
            {"authEntitySpec", ""},
            {"authEntitySpecs", ""},
            {"tokenIdsOrValues", ""},
            {"expireIn", ""},
            {"permissionIds", ""},
            {"artifactIds", ""},
            {"engineType", ""},
            {"serverQueries", ""},
            {"notes", ""},
            {"data", ""},
            {"sensorVersion", ""},
            {"qParam", ""},
            {"olderThan", ""},
            {"repository", ""},
            {"branch", ""},
            
            {"LanguageSetCommand::language", "Can we generate list of supported languages?"},
            {"AbstractProxyOptions.ProxyTargetHostsArgGroup::includedHosts", "Does it make sense to use InetAddress?"},
            {"AbstractProxyOptions.ProxyTargetHostsArgGroup::excludedHosts", "Does it make sense to use InetAddress?"},
            {"proxyHostAndPort", ""},
            {"AbstractProxyOptions::priority",""},
            {"AbstractProxyOptions::modules","Can we predefine/have modules enum?"},          
            {"TrustStoreSetCommand::trustStoreType","Can we predefine/have trustStoreType enum?"},
            {"AbstractToolInstallCommand::version", "Can we generate completion candidates from tool config file?"},
            {"AbstractToolUninstallCommand::version", "Can we generate completion candidates from tool config file?"},
            
            {"WaitHelperWaitOptions.WaitHelperWaitOptionsArgGroup::whileAll", "Ideally, each wait-for command should provide appropriate completion candidates"},
            {"WaitHelperWaitOptions.WaitHelperWaitOptionsArgGroup::whileAny", "Ideally, each wait-for command should provide appropriate completion candidates"},
            {"WaitHelperWaitOptions.WaitHelperWaitOptionsArgGroup::untilAll", "Ideally, each wait-for command should provide appropriate completion candidates"},
            {"WaitHelperWaitOptions.WaitHelperWaitOptionsArgGroup::untilAny", "Ideally, each wait-for command should provide appropriate completion candidates"},
            
            {"AbstractRestCallCommand::httpMethod", "Can we generate completion candidates/change to enum?"},
            {"SSCJobUpdateCommand::priority", "Can we provide completion candidates, like specific range of numbers?"},
            {"SSCTokenCreateCommand::type", "We can potentially provide list of commonly used token types"},
            {"SSCVulnerabilityCountCommand::groupingType", "Can we provide list of commonly used grouping types?"}
        };
        return Stream.of(altMessages).collect(Collectors.toMap(e->e[0], e->e[1]));
    }
    
    private Set<String> processedFields = new HashSet<>();
    
    /**
     * This test never fails, but prints all options and positional
     * parameters that don't have completion candidates, for manual 
     * verification whether completion candidates for such an option
     * or parameter would make sense.
     * @throws Exception
     */
    @Test
    public void testCommands() throws Exception {
        checkCommand(new CommandLine(FCLIRootCommands.class));
    }

    private void checkCommands(Map<String, CommandLine> commands) {
        if ( commands!=null && !commands.isEmpty() ) {
            commands.values().stream().forEach(this::checkCommand);
        }
    }

    private void checkCommand(CommandLine cl) {
        CommandSpec spec = cl.getCommandSpec();
        checkOptions(spec);
        checkParameters(spec);
        checkCommands(cl.getSubcommands());
    }

    private void checkOptions(CommandSpec cmdSpec) {
        cmdSpec.options().forEach(optionSpec->checkOptionCompletionCandidates(cmdSpec, optionSpec));
    }

    private void checkOptionCompletionCandidates(CommandSpec cmdSpec, OptionSpec optionSpec) {
        if ( !hasCompletionCandidates(optionSpec) ) {
            printResult(cmdSpec, optionSpec, "Option has no completion candidates");
        }
    }
    
    private void checkParameters(CommandSpec cmdSpec) {
        cmdSpec.positionalParameters().forEach(paramSpec->checkParameterCompletionCandidates(cmdSpec, paramSpec));
    }
    
    private void checkParameterCompletionCandidates(CommandSpec cmdSpec, PositionalParamSpec paramSpec) {
        if ( !hasCompletionCandidates(paramSpec) ) {
            printResult(cmdSpec, paramSpec, "Parameter has no completion candidates");
        }
    }
    
    private boolean hasCompletionCandidates(ArgSpec spec) {
        if ( !spec.hidden() && spec.completionCandidates()==null ) {
            Class<?> type = spec.type();
            if ( spec instanceof PositionalParamSpec || (type!=Boolean.TYPE && type!=Boolean.class) ) {
                if (spec.typeInfo().isMultiValue()){
                    type = spec.typeInfo().getAuxiliaryTypes()[0];
                }
                if ( !type.equals(File.class) && !type.equals(Path.class) && !type.equals(InetAddress.class)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void printResult(CommandSpec cmdSpec, ArgSpec argSpec, String msg) {
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
        var qualifiedField = getClazzName(clazz) + "::" + name;
        if ( !processedFields.contains(qualifiedField) && !isIgnored(name) ) {
            processedFields.add(qualifiedField);
            msg = fieldAltMessagesMap.getOrDefault(qualifiedField, fieldAltMessagesMap.getOrDefault(name, msg));
            if ( StringUtils.isNotBlank(msg) ) {
                System.out.println(String.format("INFO: %s: %s", qualifiedField, msg));
            }
        }
    }
    
    private boolean isIgnored(String name) {
        return Stream.of(ignoreFieldEndsWith).anyMatch(ignore->
            name.toLowerCase().endsWith(ignore.toLowerCase()));
    }
        
    private String getClazzName(Class<?> clazz) {
        var clazzName = clazz.getSimpleName();
        while ( clazz.getDeclaringClass()!=null ) {
            clazz = clazz.getDeclaringClass();
            clazzName = clazz.getSimpleName() + "." + clazzName;
        }
        return clazzName;
    }
        
    public static void main(String[] args) throws Exception {
        new CompletionCandidatesTest().testCommands();
    }
    
}
