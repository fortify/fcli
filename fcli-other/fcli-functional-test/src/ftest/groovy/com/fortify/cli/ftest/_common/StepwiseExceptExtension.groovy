package com.fortify.cli.ftest._common;

import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.extension.builtin.StepwiseExtension
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.SpecInfo

import spock.util.Exceptions

import java.lang.annotation.Annotation

/*
 * This extension allows the definition of one or multiple exceptions 
 * to the failure handling of the Stepwise annotation.
 * Errors in defined exceptions will not cause subsequent tests to be skipped.
 * Exceptions must be defined in the "except" parameter and follow this syntax:
 * "<prefix> (<className>).<featureName>"
 * Multiple exceptions can be provided separated by comma
 */
public class StepwiseExceptExtension extends StepwiseExtension {
    ArrayList<String> exceptions = Arrays.asList();
    
    public void visitSpecAnnotation(Annotation annotation, final SpecInfo spec) {
        exceptions = getExceptionsList(annotation.except())
        sortFeaturesInDeclarationOrder(spec);
        includeFeaturesBeforeLastIncludedFeature(spec);
        skipFeaturesAfterFirstFailingFeature(spec);
    }
    
    @Override
    def skipFeaturesAfterFirstFailingFeature(final SpecInfo spec) {
        spec.getBottomSpec().addListener(getRunListener(spec));
    }
      
    private AbstractRunListener getRunListener(final SpecInfo spec) {
      return new AbstractRunListener() {
        @Override
        public void error(ErrorInfo error) {
          // @StepwiseExcept only affects class that carries the annotation,
          // but not sub- and super classes
          if(exceptions.contains(error.getMethod().getParent().name + "." + error.getMethod().name)) return;
          if (!error.getMethod().getParent().equals(spec)) return;
  
          // mark all subsequent features as skipped
          List<FeatureInfo> features = spec.getFeatures();
          int indexOfFailedFeature = features.indexOf(error.getMethod().getFeature());
          for (int i = indexOfFailedFeature + 1; i < features.size(); i++) {
            features.get(i).skip("Skipped due to previous Error (by @StepwiseExcept)");
          }
        }
      }
    }
    
    private ArrayList<String> getExceptionsList(String exceptionsString){
        if(exceptionsString == null || exceptionsString.equals("")) {
            return new ArrayList<String>();
        } else {
            return Arrays.asList(exceptionsString.split(","))
        }
    }
    
    private void sortFeaturesInDeclarationOrder(SpecInfo spec) {
        for (feature in spec.getFeatures())
          feature.setExecutionOrder(feature.getDeclarationOrder());
      }
      
    private void includeFeaturesBeforeLastIncludedFeature(SpecInfo spec) {
        boolean includeRemaining = false;
        for(feature in spec.getFeatures()) {
            if (includeRemaining) feature.setExcluded(false);
            else if (!feature.isExcluded()) includeRemaining = true;
        }
      }
}
