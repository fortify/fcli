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
package com.fortify.cli.common.action.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.action.cli.mixin.ActionSourceResolverMixin;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.ActionParameterHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractActionAsciidocCommand extends AbstractRunnableCommand {
    @Mixin private ActionSourceResolverMixin.OptionalOption actionSourceResolver;
    @Mixin private CommonOptionMixins.OptionalFile outputFileMixin;
    @Option(names= {"--manpage-dir", "-d"}, required = false, descriptionKey="fcli.action.asciidoc.manpage-dir")
    private Path manpageDir;
    
    @Override @SneakyThrows
    public final Integer call() {
        initMixins();
        var contents = generateHeader();
        contents += ActionLoaderHelper
            .streamAsActions(actionSourceResolver.getActionSources(getType()), ActionValidationHandler.IGNORE)
            .map(this::generateActionSection)
            .collect(Collectors.joining("\n\n"));
        contents = addLinks(contents);
        var outputFile = outputFileMixin.getFile();
        if ( outputFile==null ) {
            System.out.println(contents);
        } else {
            // TODO Should we require confirmation is file already exists?
            Files.writeString(outputFile.toPath(), contents, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return 0;
    }
    
    private final String replaceVariables(String s) {
        return s.replace("${version}", FcliBuildPropertiesHelper.getFcliBuildInfo().replace(':', ' '))
                .replace("${type}", getType())
                .replace("${typeLower}", getType().toLowerCase());
    }
    
    private final String generateHeader() {
        return replaceVariables("""
                = Fcli ${type} Actions
                
                This manual page describes built-in fcli ${type} actions that can be run through
                the `fcli ${typeLower} action run <action-name>` command.
                
                """);
    }
    
    private final String generateActionSection(Action action) {
        // TODO Generate proper options list in synopsis. We should have a re-usable method in
        //      ActionParameterHelper or other class for generating this, such that we can also
        //      show synopsis in `fcli * action help` output.
        String name = action.getMetadata().getName();
        return replaceVariables(String.format("""
            == %s
            
            %s
            
            === Synopsis
            
            *fcli ${typeLower} action run %s <options>* 
            
            === Description
            
            %s
            
            === Options
            
            %s
            
            """, name, action.getUsage().getHeader(), name, action.getUsage().getDescription(), generateOptionsSection(action)));
    }
    
    private final String generateOptionsSection(Action action) {
        return ActionParameterHelper.getOptionDescriptors(action)
            .stream().map(this::generateOptionDescription).collect(Collectors.joining("\n\n"));
    }
    
    private final String generateOptionDescription(IOptionDescriptor descriptor) {
        return String.format("%s::\n%s", 
                descriptor.getOptionNamesAndAliasesString(", "), 
                StringUtils.indent(descriptor.getDescription(), "  "));
    }
    
    private final String addLinks(String contents) {
        if ( manpageDir==null ) { return contents; }
        var manPages = listDir(manpageDir).stream().filter(s->s.matches("fcli-[\\w-]+-[\\w-]+-[\\w-]+.adoc"))
            .map(s->s.replaceAll("\\.adoc", ""))
            .collect(Collectors.toSet());
        for ( var manPage : manPages ) {
            var pattern = manPage.replace("-", "[ -]");
            var replacement = String.format("link:manpage/%s.html[$1]", manPage);
            contents = contents.replaceAll("(?<!`)("+pattern+")", replacement);
            contents = contents.replaceAll("(`"+pattern+".*`)", replacement);
        }
        return contents;
    }

    private Set<String> listDir(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
              .filter(file -> !Files.isDirectory(file))
              .map(Path::getFileName)
              .map(Path::toString)
              .collect(Collectors.toSet());
        } catch ( IOException e ) {
            return new HashSet<>();
        }
    }
    
    protected abstract String getType();
    
    
}
