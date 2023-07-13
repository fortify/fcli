/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.ftest._common.extension;

import static java.util.Arrays.asList

import org.spockframework.runtime.IStandardStreamsListener
import org.spockframework.runtime.StandardStreamsCapturer
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.FieldInfo;
import org.spockframework.runtime.model.SpecInfo;

import com.fortify.cli.ftest._common.spec.FcliOutput;

import groovy.transform.CompileStatic;

@CompileStatic
public class FcliOutputExtension implements IGlobalExtension {
    @Override
    public void visitSpec(SpecInfo spec) {
        spec.allFields.each { FieldInfo field ->
            if ( field.getAnnotation(FcliOutput.class) ) {
                def capturer = new FcliOutputCapturer();
                spec.addInitializerInterceptor {
                    it.instance.metaClass.setProperty(it.instance, field.reflection.name, capturer)
                    capturer.start()
                    it.proceed()
                }
                spec.addCleanupInterceptor {
                    capturer.stop()
                    it.instance.metaClass.setProperty(it.instance, field.reflection.name, null)
                    it.proceed()
                }
            }
        }
    }
    
    @CompileStatic
    private class FcliOutputCapturer implements IStandardStreamsListener {
        private StandardStreamsCapturer capturer;
        private StringBuffer buffer = null;
        
        @Override
        public void standardOut(String message) {
            buffer.append(message)
        }
        
        @Override
        public void standardErr(String message) {
            buffer.append(message)
        }
        
        public List<String> getLines() {
            return asList(buffer.toString().split("(\\r\\n|\\n|\\r)"));
        }
        
        void start() {
            this.buffer = new StringBuffer()
            this.capturer = new StandardStreamsCapturer();
            this.capturer.addStandardStreamsListener(this);
            this.capturer.start()
        }
        void stop() {
            this.capturer.stop()
            this.capturer = null
            this.buffer = null
        }
    } 
}
