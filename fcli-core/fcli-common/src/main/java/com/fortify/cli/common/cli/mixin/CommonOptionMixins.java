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

import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.Getter;
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
}
