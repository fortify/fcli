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
abstract class AbstractTempDirExtension implements IGlobalExtension {
    private Path tempDir
    
    public final Path getTempDir() { return tempDir }
    
    @Override
    public final void start() {
         tempDir = TempDirHelper.create()
         afterCreate()
    }
    
    @Override
    public final void stop() {
        if ( !System.getProperty("ft.keep-temp-dirs") ) {
            TempDirHelper.rm(tempDir)
        }
    }
    
    public void visitSpec(SpecInfo spec) {
        // Register an interceptor for setting up any fields for which we can determine a path
        spec.allFields.each { FieldInfo field ->
            def path = getPathForField(field)
            if ( path ) {
                if ( field.shared ) {
                    spec.addSharedInitializerInterceptor {
                        setPathOnField(field, it.instance, path)
                        it.proceed()
                    }
                } else {
                    spec.addSetupInterceptor {
                        setPathOnField(field, it.instance, path)
                        it.proceed()
                    }
                }
            }
        }
    }
    
    protected void afterCreate() {}
    protected abstract Path getPathForField(FieldInfo field);

    private final void setPathOnField(FieldInfo field, Object instance, Path path) {
        def type = field.reflection.type
        if ( type.isAssignableFrom(Path) ) {
            instance.metaClass.setProperty(instance, field.reflection.name, path)
        } else if ( type.isAssignableFrom(File) ) {
            instance.metaClass.setProperty(instance, field.reflection.name, path.toFile())
        } else if ( type.isAssignableFrom(String) ) {
            instance.metaClass.setProperty(instance, field.reflection.name, path.toFile().absolutePath)
        } else {
            throw new RuntimeException("Only Path, File or String fields are supported, not "+type)
        }
    }
}