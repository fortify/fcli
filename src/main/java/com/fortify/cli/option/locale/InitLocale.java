package com.fortify.cli.option.locale;

import picocli.CommandLine;
import java.util.List;
import java.util.Locale;

/**
 * TODO: This is temporary. Will need to implement a "locale" command to persist changes to the default language.
 */
public class InitLocale implements Runnable {

    @CommandLine.Option(names = { "-l", "--locale" }, description = "Manually specify the local for fcli help information.")
    void setLocale(String locale) {
        Locale.setDefault(new Locale(locale));
    }

    @CommandLine.Unmatched
    List<String> remainder; // ignore any other parameters and options in the first parsing phase

    @Override
    public void run() {
    }
}
