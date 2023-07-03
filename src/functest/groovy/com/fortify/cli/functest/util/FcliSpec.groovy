package com.fortify.cli.functest.util;

import com.fortify.cli.app.FortifyCLI

import io.github.joke.spockoutputcapture.CapturedOutput
import io.github.joke.spockoutputcapture.OutputCapture
import spock.lang.Specification

class FcliSpec extends Specification {
    @OutputCapture CapturedOutput out
    
    def fcli(String... args) {
        // TODO Set this property globally, for example in global extension
        //      (which is also where we can initialize Micronaut & CommandLine)
        System.setProperty("jansi.disable", "true");
        System.setProperty("org.fusesource.jansi.Ansi.disable", "true");
        FortifyCLI.execute(args)==0
    }
}

