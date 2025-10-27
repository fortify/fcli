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
package com.fortify.cli.common.action.cli.cmd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.action.cli.mixin.ActionSourceResolverMixin;
import com.fortify.cli.common.action.helper.ActionDescriptionRenderer;
import com.fortify.cli.common.action.helper.ActionDescriptionRenderer.ActionDescriptionRendererType;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.processor.ActionCliOptionsProcessor.ActionOptionHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.StringHelper;

import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractActionAsciidocCommand extends AbstractRunnableCommand {
    private static final ActionDescriptionRenderer descriptionRenderer = 
            ActionDescriptionRenderer.create(ActionDescriptionRendererType.ASCIIDOC);
    @Mixin private ActionSourceResolverMixin.OptionalOption actionSourceResolver;
    @Mixin private CommonOptionMixins.OptionalFile outputFileMixin;
    @Option(names= {"--manpage-dir", "-d"}, required = false, descriptionKey="fcli.action.asciidoc.manpage-dir")
    private Path manpageDir;
    
    @Override @SneakyThrows
    public final Integer call() {
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
        return s.replace("${version}", FcliBuildProperties.INSTANCE.getFcliBuildInfo().replace(':', ' '))
                .replace("${type}", getType())
                .replace("${actionCmd}", getActionCmd());
    }
    
    private final String generateHeader() {
        return replaceVariables(String.format("""
                = Action Documentation: `%s`
                
                This manual page describes built-in fcli actions that can be run through
                the `%s run <action-name>` command.
                
                """, getActionCmd(), getActionCmd()));
    }
    
    private final String generateActionSection(Action action) {
        // TODO Generate proper options list in synopsis. We should have a re-usable method in
        //      ActionParameterHelper or other class for generating this, such that we can also
        //      show synopsis in `fcli * action help` output.
        String name = action.getMetadata().getName();
        var result = replaceVariables(String.format("""
            == %s
            
            %s
            
            === Synopsis
            
            *${actionCmd} run %s [${actionCmd} run options] [action options, see below]* 
            
            === Description
            
            %s
            
            """
            , name, action.getUsage().getHeader(), name, descriptionRenderer.render(action.getUsage().getDescription())));
        var optionsSection = replaceVariables(generateOptionsSection(action));
        if ( StringUtils.isNotBlank(optionsSection) ) {
            result += String.format("=== Options\n\n%s\n\n", optionsSection);
        }
        return result;
    }

    @SneakyThrows
    private String getClasspathResourceAsString(String path) {
        try ( var is = this.getClass().getResourceAsStream(path) ) {
            if ( is==null ) {
                throw new FcliBugException(String.format("Class path resource %s not found", path));
            }
            return String.format("\n----\n%s\n----\n", IOUtils.toString(is, StandardCharsets.UTF_8));
        }
    }
    
    private final String generateOptionsSection(Action action) {
        return ActionOptionHelper.getOptionDescriptors(action)
            .stream().map(this::generateOptionDescription).collect(Collectors.joining("\n\n"));
    }
    
    private final String generateOptionDescription(IOptionDescriptor descriptor) {
        return String.format("%s::\n%s", 
                descriptor.getOptionNamesString(", "),
                StringHelper.indent(descriptor.getDescription(), "  "));
    }
    
    private final String addLinks(String contents) {
        if ( manpageDir==null ) { return contents; }
        // TODO Do we want to automatically insert fcli links (which could potentially lead to bugs as seen with 
        //      https://github.com/fortify/fcli/issues/622), or should we allow Markdown syntax in action descriptions?
        //      We could either add support for new markdownDescription properties, or allow Markdown in existing
        //      description properties and clean this up in the 'action help' command.
        var manPages = listDir(manpageDir).stream().filter(s->s.matches("fcli-.*\\.adoc"))
            .map(s->s.replaceAll("\\.adoc", ""))
            .sorted((a,b)->Integer.compare(b.length(), a.length())) // In case of overlapping names, we need to replace longest matching name first
            .collect(Collectors.toList());
        for ( var manPage : manPages ) {
            var pattern = manPage.replace("-", "[ -]");
            var replacement = String.format("link:manpage/%s.html[$1]", manPage);
            if ( manPage.matches("fcli-[a-z-]+-action-run|fcli-action-run") ) {
                // Replace 'fcli <module> action run' references in synopsis
                contents = contents.replaceAll("("+pattern+")", replacement);  
            } else {
                // Replace literal 'fcli *' references embedded in backticks, if not preceded by '['
                // as that (likely) means we already generated a link for a longer command name.
                // See https://github.com/fortify/fcli/issues/622 for example. The backticks need to
                // go into the link text (as otherwise link:... would be rendered literally), so we
                // need to include the full text between the backticks in the link text.
                contents = contents.replaceAll("(?<!\\[)(`"+pattern+"[^`]*`)", replacement);
            }
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
    protected abstract String getActionCmd();
    
}
