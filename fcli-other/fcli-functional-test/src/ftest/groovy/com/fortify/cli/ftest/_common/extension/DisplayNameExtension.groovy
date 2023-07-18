package com.fortify.cli.ftest._common.extension;

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.spec.Prefix

import groovy.transform.CompileStatic

@CompileStatic
class DisplayNameExtension implements IGlobalExtension {
    @Override
    void visitSpec(SpecInfo spec) {
        def prefixAnnotation = spec.getAnnotation(Prefix.class)
        if ( prefixAnnotation ) {
            spec.allFeatures.each { 
                it.name = prefixAnnotation.value()+"."+it.name
            }
            spec.name = prefixAnnotation.value()+" ("+spec.name+")"
        }
    }
}