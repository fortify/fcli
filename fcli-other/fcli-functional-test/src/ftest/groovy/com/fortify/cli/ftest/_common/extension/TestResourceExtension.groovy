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
class TestResourceExtension implements IGlobalExtension {
    private Path resourceDir
    @Override
    public void start() {
         resourceDir = TempDirHelper.create()
         println("Using test resources directory "+resourceDir)
    }
    
    @Override
    public void stop() {
        TempDirHelper.rm(resourceDir)
    }
    
    public void visitSpec(SpecInfo spec) {
        // Register an interceptor for setting up any fields annotated with @TestResource
        spec.allFields.each { FieldInfo field ->
            def annotation = field.getAnnotation(TestResource.class)
            if ( annotation ) {
                Path path = extractResource(annotation.value())
                if ( field.shared ) { 
                    spec.addSharedInitializerInterceptor {
                        setTestResourceField(field, it.instance, path)
                        it.proceed()
                    }
                } else { 
                    spec.addSetupInterceptor {
                        setTestResourceField(field, it.instance, path)
                        it.proceed()
                    }
                }
            }
        }
    }

    private void setTestResourceField(FieldInfo field, Object instance, Path path) {
        def type = field.reflection.type
        if ( type.isAssignableFrom(Path) ) {
            instance.metaClass.setProperty(instance, field.reflection.name, path)
        } else if ( type.isAssignableFrom(File) ) {
            instance.metaClass.setProperty(instance, field.reflection.name, path.toFile())
        } else if ( type.isAssignableFrom(String) ) {
            instance.metaClass.setProperty(instance, field.reflection.name, path.toFile().absolutePath)
        } else {
            throw new RuntimeException("@TestResource annotation is only supported on Path, File or String fields, not "+type)
        }
    }
    
    private Path extractResource(String resourceFile) {
        TestResourceExtension.class.classLoader.getResourceAsStream(resourceFile).withCloseable {
            Path outputFilePath = resourceDir.resolve(resourceFile)
            outputFilePath.parent.toFile().mkdirs()
            Files.copy(it, outputFilePath, StandardCopyOption.REPLACE_EXISTING)
            return outputFilePath
        }
    }
    
}