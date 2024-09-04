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
package com.fortify.cli.common.cli.mixin;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

public class CommonOptionMixins {
    private CommonOptionMixins() {}
    
    public static class OptionalFile {
        @Option(names = {"-f", "--file"})
        @Getter private File file;
    }
    
    public static class RequiredFile {
        @Option(names = {"-f", "--file"}, required=true)
        @Getter private File file;
    }
    
    public static class RequireConfirmation {
        @Mixin private CommandHelperMixin commandHelper;
        @Option(names = {"-y", "--confirm"}, defaultValue = "false")
        private boolean confirmed;
        
        public void checkConfirmed(Object... promptArgs) {
            if (!confirmed) {
                CommandSpec spec = commandHelper.getCommandSpec();
                if ( System.console()==null ) {
                    throw new ParameterException(spec.commandLine(), 
                            PicocliSpecHelper.getRequiredMessageString(spec, 
                                    "error.missing.confirmation", getPlainPrompt(spec, promptArgs).replace("\n", "\n  ")));
                } else {
                    String expectedResponse = PicocliSpecHelper.getRequiredMessageString(spec, "expectedConfirmPromptResponse");
                    String response = System.console().readLine(getPrompt(spec, promptArgs));
                    if ( response.equalsIgnoreCase(expectedResponse) ) {
                        return;
                    } else {
                        throw new AbortedByUserException("Aborting: operation aborted by user");
                    }
                }
            }
        }
        
        private String getPrompt(CommandSpec spec, Object... promptArgs) {
            String prompt = getPlainPrompt(spec, promptArgs);
            String promptOptions = PicocliSpecHelper.getRequiredMessageString(spec, "confirmPromptOptions");
            return String.format("%s (%s) ", prompt, promptOptions);
        }

        private String getPlainPrompt(CommandSpec spec, Object... promptArgs) {
            String prompt = PicocliSpecHelper.getMessageString(spec, "confirmPrompt", promptArgs);
            if ( StringUtils.isBlank(prompt) ) {
                String[] descriptionLines = spec.optionsMap().get("-y").description();
                if ( descriptionLines==null || descriptionLines.length<1 ) {
                    throw new RuntimeException("No proper description found for generating prompt for --confirm option");
                }
                prompt = spec.optionsMap().get("-y").description()[0].replaceAll("[. ]+$", "")+"?";
            }
            return prompt;
        }
        
        public static final class AbortedByUserException extends IllegalStateException {
            private static final long serialVersionUID = 1L;
            public AbortedByUserException(String msg) { super(msg); }
        }
    }
    
    public static abstract class AbstractTextResolverMixin {
        public abstract String getTextSource();
        
        @RequiredArgsConstructor
        private static enum TextSources {
            file(TextSources::resolveFile), 
            url(TextSources::resolveUrl), 
            string(TextSources::resolveString), 
            env(TextSources::resolveEnv);
            
            private final Function<String, String> resolver;
            
            public static final String resolve(String source) {
                if ( source==null ) { return null; }
                for ( var type : values() ) {
                    var prefix = type.name()+":";
                    if ( source.startsWith(prefix) ) {
                        return type.resolver.apply(source.replaceFirst(prefix, ""));
                    }
                }
                return resolveFile(source);
            }
            
            @SneakyThrows
            private static final String resolveFile(String file) {
                // As '~' will not be resolved by the shell due to the 'file:'
                // prefix, we resolve this manually to user home directory.
                file = file.replaceFirst("^~", EnvHelper.getUserHome());
                return Files.readString(Path.of(file));
            }
            
            @SneakyThrows
            private static final String resolveUrl(String url) {
                return IOUtils.toString(new URL(url), StandardCharsets.US_ASCII);
            }
            
            private static final String resolveString(String string) {
                return string;
            }
            
            private static final String resolveEnv(String envName) {
                return System.getenv(envName);
            }
        }
        
        public final String getText() {
            return TextSources.resolve(getTextSource());
        }
    }
}
