package com.fortify.cli.ftest.ssc._common

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException

class SSCCustomTagSupplier implements AutoCloseable {
    private final Closure init
    private SSCCustomTag tag

    SSCCustomTagSupplier() {
        this({})
    }

    SSCCustomTagSupplier(Closure init) {
        this.init = init
    }

    SSCCustomTag getTag() {
        if (!tag) {
            tag = new SSCCustomTag().create()
            init(tag)
        }
        return tag
    }

    @Override
    void close() {
        if (tag) {
            tag.close()
            tag = null
        }
    }

    static class SSCCustomTag {
        private final String random = System.currentTimeMillis()
        private final String fcliVariableName = "ssc_customtag_" + random
        private String id
        private String name
        private String type

        SSCCustomTag create(String valueType = "TEXT") {
            name = "ftest-tag-" + random
            def args = "ssc custom-tag create ${name} --type=${valueType} --store ${fcliVariableName}"
            Fcli.run(args) { it.expectSuccess(true, "Unable to create custom tag") }
            id = get("id")
            type = valueType
            return this
        }

        String get(String propertyPath) {
            Fcli.run("util var contents $fcliVariableName -o expr={$propertyPath}") {
                it.expectSuccess(true, "Error getting custom tag property $propertyPath")
            }.stdout[0]
        }

        String getVariableName() { fcliVariableName }
        String getVariableRef() { "::${fcliVariableName}::" }
        String getId() { id }
        String getName() { name }
        String getType() { type }

        void close() {
            Fcli.run("ssc custom-tag delete ::$fcliVariableName::") {
                it.expectSuccess(true, "Unable to delete custom tag")
            }
        }
    }
}