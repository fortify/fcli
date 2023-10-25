package com.fortify.cli.ftest._common.extension;

import java.nio.file.Path

import org.spockframework.runtime.model.FieldInfo

import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TempFile

import groovy.transform.CompileStatic

@CompileStatic
class TempPathExtension extends AbstractTempDirExtension {
    @Override
    protected void afterCreate() {
        println "Using temporary working directory $tempDir"
    }
    
    @Override
    protected Path getPathForField(FieldInfo field) {
        Path result = null
        def dirAnnotation = field.getAnnotation(TempDir.class)
        if ( dirAnnotation!=null ) {
            result = tempDir.resolve(dirAnnotation.value())
            result.toFile().mkdirs() // Make sure directory exists
        } else {
            def fileAnnotation = field.getAnnotation(TempFile.class)
            if ( fileAnnotation!=null ) {
                result = tempDir.resolve(fileAnnotation.value())
                result.parent.toFile().mkdirs() // Make sure parent directory exists
            }
        }
        return result
    }
}