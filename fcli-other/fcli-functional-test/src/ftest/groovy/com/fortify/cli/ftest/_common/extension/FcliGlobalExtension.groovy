package com.fortify.cli.ftest._common.extension;

import java.nio.file.Path

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecElementInfo
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Input
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Global.IGlobalValueSupplier
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TempFile
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest._common.util.WorkDirHelper

import groovy.transform.CompileStatic

@CompileStatic
public class FcliGlobalExtension implements IGlobalExtension {
    private WorkDirHelper workDirHelper;
    private Map<Class<? extends IGlobalValueSupplier>, IGlobalValueSupplier> globalValueSuppliers = new HashMap<>();

    @Override
    public void start() {
        workDirHelper = new WorkDirHelper();
        Fcli.initialize(workDirHelper.getFortifyDataDir());
    }
    
    @Override
    public void visitSpec(SpecInfo spec) {
        updateSpecName(spec);
        skipFeatures(spec);
        initFcliSessions(spec);
        if ( !spec.skipped ) {
            spec.allFields.each { FieldInfo field -> initializeField(spec, field) }
        }
    }
    
    @Override
    public void stop() {
        globalValueSuppliers.values().each { s -> s.close() }
        FcliSessionType.logoutAll()
        Fcli.close()
        workDirHelper.close();
    }
    
    private void updateSpecName(SpecInfo spec) {
        def prefixAnnotation = spec.getAnnotation(Prefix.class)
        if ( prefixAnnotation ) {
            spec.allFeatures.each {
                it.name = prefixAnnotation.value()+"."+it.name
            }
            spec.name = prefixAnnotation.value()+" ("+spec.name+")"
        }
    }
    
    private void skipFeatures(SpecInfo spec) {
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
            skipSpec(spec); // Skip spec if all features skipped
        }
    }
    
    private void skipSpec(SpecInfo spec) {
        if ( !spec.allFeatures.findAll({ f->!f.skipped }) ) {
            spec.skip "All features skipped"
        }
    }
    
    private void initFcliSessions(SpecInfo spec) {
        if ( !spec.skipped ) {
            initFcliSession(spec, spec.getAnnotation(FcliSession.class));
            spec.allFeatures.each({ feature ->
                if ( !feature.skipped ) { 
                    initFcliSession(feature, feature.featureMethod.getAnnotation(FcliSession.class));
                }
            })
            skipSpec(spec)
        }
    }
    
    private void initFcliSession(SpecElementInfo elt, FcliSession annotation) {
        if ( annotation ) {
            annotation.value().each {
                def handler = it.handler
                if ( !elt.excluded && !elt.skipped ) {
                    if (handler.isEnabled() ) {
                        if ( !handler.login() ) {
                            elt.skip "Skipped due to "+handler.friendlyName()+" login failure"
                        }
                    } else {
                        elt.skip "No "+handler.friendlyName()+ " session available";
                    }
                }
            }
        }
    }
    
    private void initializeField(SpecInfo spec, FieldInfo field) {
        addFieldTempDirInterceptor(spec, field);
        addFieldTempFileInterceptor(spec, field);
        addFieldResourceFileInterceptor(spec, field);
        addFieldGlobalInterceptor(spec, field);
    }
    
    private final void addFieldTempDirInterceptor(SpecInfo spec, FieldInfo field) {
        def tempDirAnnotation = field.getAnnotation(TempDir.class)
        if ( tempDirAnnotation!=null ) {
            addFieldInterceptor(spec, field, convertPath(field, workDirHelper.getTempDir(tempDirAnnotation.value())))
        }
    }
    
    private final void addFieldTempFileInterceptor(SpecInfo spec, FieldInfo field) {
        def tempFileAnnotation = field.getAnnotation(TempFile.class)
        if ( tempFileAnnotation!=null ) {
            addFieldInterceptor(spec, field, convertPath(field, workDirHelper.getTempFile(tempFileAnnotation.value())))
        }
    }
    
    private final void addFieldResourceFileInterceptor(SpecInfo spec, FieldInfo field) {
        def resourceFileAnnotation = field.getAnnotation(TestResource.class)
        if ( resourceFileAnnotation!=null ) {
            addFieldInterceptor(spec, field, convertPath(field, workDirHelper.getResource(resourceFileAnnotation.value())))
        }
    }
    
    private final void addFieldGlobalInterceptor(SpecInfo spec, FieldInfo field) {
        def globalAnnotation = field.getAnnotation(Global.class)
        if ( globalAnnotation!=null ) {
            def clazz = globalAnnotation.value();
            def valueSupplier = globalValueSuppliers.computeIfAbsent(clazz, {c->c.newInstance()});
            addFieldInterceptor(spec, field, valueSupplier.getValue(workDirHelper));
        }
    }
    
    private final void addFieldInterceptor(SpecInfo spec, FieldInfo field, Object value) {
        if ( value ) {
            if ( field.shared ) {
                spec.addSharedInitializerInterceptor {
                    it.instance.metaClass.setProperty(it.instance, field.reflection.name, value)
                    it.proceed()
                }
            } else {
                spec.addSetupInterceptor {
                    it.instance.metaClass.setProperty(it.instance, field.reflection.name, value)
                    it.proceed()
                }
            }
        }
    }
    
    private final Object convertPath(FieldInfo field, Path path) {
        def type = field.reflection.type
        if ( type.isAssignableFrom(Path) ) {
            return path;
        } else if ( type.isAssignableFrom(File) ) {
            return path.toFile();
        } else if ( type.isAssignableFrom(String) ) {
            return path.toFile().absolutePath;
        } else {
            throw new RuntimeException("Only Path, File or String fields are supported, not "+type)
        }
    }
}