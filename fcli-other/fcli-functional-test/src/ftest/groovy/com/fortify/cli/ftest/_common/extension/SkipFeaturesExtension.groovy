package com.fortify.cli.ftest._common.extension;

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.Input

import groovy.transform.CompileStatic

@CompileStatic
class SkipFeaturesExtension implements IGlobalExtension {
    @Override
    void visitSpec(SpecInfo spec) {
        // Exclude any features not matching any of the feature names listed in 
        // the fcli.run property
        // TODO Add support for skipping features based on tag include/exclude
        //      expressions
        def run = Input.TestsToRun.get()?.split(",")
        if (run) {
            spec.allFeatures.each({ feature ->
                if ( !run.any {feature.name.startsWith(it) && !feature.skipped } ) {
                    feature.skip "Not included in "+Input.TestsToRun.propertyName()+" property"
                }
            })
            if ( !spec.allFeatures.findAll({ f->!f.skipped }) ) {
                spec.skip "All features skipped"
            }
        }
    }
}