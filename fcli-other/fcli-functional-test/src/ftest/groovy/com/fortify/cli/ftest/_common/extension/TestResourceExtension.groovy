package com.fortify.cli.ftest._common.extension;

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.spec.TestResource
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
        return annotation==null ? null : extractResource(annotation.value())
    }
    
    private Path extractResource(String resourceFile) {
        getResource(resourceFile).withCloseable {
            Path outputFilePath = tempDir.resolve(resourceFile)
            outputFilePath.parent.toFile().mkdirs()
            Files.copy(it, outputFilePath, StandardCopyOption.REPLACE_EXISTING)
            return outputFilePath
        }
    }
    
    private InputStream getResource(String resourceFile) {
        def cl = TestResourceExtension.class.classLoader
        def stream = cl.getResourceAsStream(resourceFile)
        if ( stream==null ) {
            stream = cl.getResourceAsStream(resourceFile+"-no-shadow")
        }
        if ( stream==null ) {
            throw new IllegalStateException("${resourceFile} (or ${resourceFile}-no-shadow) referenced in @TestResource not found")
        }
        return stream
    }
    
}