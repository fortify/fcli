package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.query")
class QuerySpec extends FcliBaseSpec {
    def "contains"() {
        expect:
            // TODO Implement this (and many other) spec, using sample data from
            //      "fcli util sample-data list", making sure that we cover for 
            //      example https://github.com/fortify/fcli/issues/344
            true
    }
}