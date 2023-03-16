package com.fortify.cli.common.cli.mixin;

import com.fortify.cli.common.util.CommandSpecHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class CommonOptionMixins {
    private CommonOptionMixins() {}
    
    public static class OptionalDestinationFile {
        @Option(names = {"-f", "--dest"}, descriptionKey = "fcli.destination-file")
        @Getter private String destination;
    }
    
    public static class RequireConfirmation {
        @Spec(Target.MIXEE) CommandSpec spec;
        @Option(names = {"-y", "--confirm"}, defaultValue = "false")
        private boolean confirmed;
        
        public void checkConfirmed() {
            if (!confirmed) {
                if ( System.console()==null ) {
                    throw new ParameterException(spec.commandLine(), "Missing option: Confirm operation with -y / --confirm (interactive prompt not available)");
                } else {
                    String expectedResponse = CommandSpecHelper.getRequiredMessageString(spec, "expectedConfirmPromptResponse");
                    String response = System.console().readLine(getPrompt());
                    if ( response.equalsIgnoreCase(expectedResponse) ) {
                        return;
                    } else {
                        throw new IllegalStateException("Aborting: operation aborted by user");
                    }
                }
            }
        }
        
        private String getPrompt() {
            String prompt = CommandSpecHelper.getMessageString(spec, "confirmPrompt");
            if ( StringUtils.isBlank(prompt) ) {
                String[] descriptionLines = spec.optionsMap().get("-y").description();
                if ( descriptionLines==null || descriptionLines.length<1 ) {
                    throw new RuntimeException("No proper description found for generating prompt for --confirm option");
                }
                prompt = spec.optionsMap().get("-y").description()[0]+"?";
            }
            String promptOptions = CommandSpecHelper.getRequiredMessageString(spec, "confirmPromptOptions");
            return String.format("%s (%s) ", prompt, promptOptions);
        }
    }
}
