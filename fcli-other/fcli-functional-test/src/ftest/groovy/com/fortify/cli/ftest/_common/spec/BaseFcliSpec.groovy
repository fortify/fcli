package com.fortify.cli.ftest._common.spec;

import com.fortify.cli.ftest._common.extension.FcliOutputExtension.FcliOutputCapturer

import spock.lang.Specification

class BaseFcliSpec extends Specification {
    @FcliOutput FcliOutputCapturer out
    @Fcli fcli
}

