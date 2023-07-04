package com.fortify.cli.functest.common.spec;

import io.github.joke.spockoutputcapture.CapturedOutput
import io.github.joke.spockoutputcapture.OutputCapture
import spock.lang.Specification

class BaseFcliSpec extends Specification {
    @OutputCapture CapturedOutput out
    @Fcli fcli
}

