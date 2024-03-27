package com.fortify.cli.ftest._common.extension;

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest._common.util.RuntimeResourcesHelper
import com.fortify.cli.ftest._common.util.TempDirHelper

import groovy.transform.CompileStatic

@CompileStatic
class TestResourceExtension extends AbstractTempDirExtension {
    @Override
    protected void afterCreate() {
        println "Using test resources directory $tempDir"
    }
    
    @Override
    protected Path getPathForField(FieldInfo field) {
        def annotation = field.getAnnotation(TestResource.class)
        return annotation==null ? null : RuntimeResourcesHelper.extractResource(annotation.value())
    }
    
    
    
}