package com.fortify.cli.ftest._common.extension;

import org.spockframework.runtime.extension.IAnnotationDrivenExtension
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.SpecElementInfo
import org.spockframework.runtime.model.SpecInfo

import com.fortify.cli.ftest._common.spec.FcliSession

public class FcliSessionExtension implements IAnnotationDrivenExtension<FcliSession> {
    @Override
    public void visitSpecAnnotation(FcliSession annotation, SpecInfo spec) {
        visit(annotation, spec)
    }
  
    @Override
    public void visitFeatureAnnotation(FcliSession annotation, FeatureInfo feature) {
        visit(annotation, feature)  
    }
    
    private void visit(FcliSession annotation, SpecElementInfo elt) {
        def handler = annotation.value().handler
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