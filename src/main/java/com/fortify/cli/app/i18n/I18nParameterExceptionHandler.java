package com.fortify.cli.app.i18n;

import java.util.ResourceBundle;

import com.fortify.cli.common.i18n.helper.LanguageHelper;

import picocli.CommandLine;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParameterException;

public class I18nParameterExceptionHandler implements CommandLine.IParameterExceptionHandler {
    private final IParameterExceptionHandler origDefaultHandler;
    private final ResourceBundle i18nResource;


    /**
     * This constructor will setup the custom Picocli Parameter Exception Handler so that error messages can be
     * localized. The constructor needs the original default exception handler from Picocli so that if there's an error
     * that does not have a localized version of the error message, then the original error message in English can still
     * be displayed. Finally, access to {@link LanguagePropertiesManager} will provide access to the appropriate resource file.
     * @param origDefaultHandler
     * @param languageConfigManager
     */
    public I18nParameterExceptionHandler(IParameterExceptionHandler origDefaultHandler){
        this.origDefaultHandler = origDefaultHandler;
        String resourceBundleName = "com.fortify.cli.common.i18n.FortifyCLIMessages";
        i18nResource = ResourceBundle.getBundle(resourceBundleName, LanguageHelper.getConfiguredLanguageDescriptor().getLocale());
    }

    /**
     * A custom handler to allow for internationalization of important error messages.
     * Handles a {@code ParameterException} that occurred and returns an exit code.
     *
     * @param ex   the ParameterException describing the problem that occurred while parsing the command line arguments,
     *             and the CommandLine representing the command or subcommand whose input was invalid
     * @param args the command line arguments that could not be parsed
     * @return an exit code
     */
    @Override
    public int handleParseException(ParameterException ex, String[] args) throws Exception {
        // TODO: Add additional cases so that all standard error messages can be handled.
        if (ex != null && ex.getMessage().contains("Missing required subcommand")){
            String msg = i18nResource.getString("error.missing.subcommand");
            CommandLine cmd = ex.getCommandLine();
            cmd.getErr().println(msg);
            cmd.usage(cmd.getErr());
            return cmd.getCommandSpec().exitCodeOnInvalidInput();
        } else if(ex != null && ex.getMessage().contains("Missing required parameter:")){
            String msg = i18nResource.getString("error.missing.parameter");
            CommandLine cmd = ex.getCommandLine();
            cmd.getErr().println(msg + ex.getMessage().split(":")[1]);
            cmd.usage(cmd.getErr());
            return cmd.getCommandSpec().exitCodeOnInvalidInput();
        }
        // returns the original default handler and print things normally
        return origDefaultHandler.handleParseException(ex, args);
    }
}
