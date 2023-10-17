/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.ResourceBundle;

import com.fortify.cli.common.cli.util.CommandGroup;

import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.Messages;

public class PicocliSpecHelper {
    public static final <T extends Annotation> T findAnnotation(CommandSpec commandSpec, Class<T> annotationType) {
        T annotation = null;
        while ( commandSpec!=null && annotation==null ) {
            Object cmd = commandSpec.userObject();
            annotation = cmd==null ? null : cmd.getClass().getAnnotation(annotationType);
            commandSpec = commandSpec.parent();
        }
        return annotation;
    }
    
    public static final <T extends Annotation> T getAnnotation(CommandSpec cmdSpec, Class<T> annotationType) {
        var userObject = cmdSpec==null ? null : cmdSpec.userObject();
        if ( userObject!=null ) {
            return userObject.getClass().getAnnotation(annotationType);
        }
        return null;
    }
    
    public static final <T extends Annotation> T getAnnotation(ArgSpec argSpec, Class<T> annotationType) {
        var userObject = argSpec==null ? null : argSpec.userObject();
        if ( userObject!=null && userObject instanceof AccessibleObject ) {
            return ((AccessibleObject) userObject).getAnnotation(annotationType);
        }
        return null;
    }
    
    public static final String getCommandGroup(CommandSpec cmdSpec) {
        var annotation = getAnnotation(cmdSpec, CommandGroup.class);
        return annotation==null ? null : annotation.value();
    }
    
    public static final String getMessageString(CommandSpec commandSpec, String keySuffix) {
        var group = getCommandGroup(commandSpec);
        Messages messages = getMessages(commandSpec);
        String value = null;
        while ( commandSpec!=null && value==null ) {
            String pfx = commandSpec.qualifiedName(".")+".";
            value = getMessageString(messages, pfx, group, keySuffix);
            commandSpec = commandSpec.parent();
        }
        // If value is still null, try without any prefix
        return value!=null ? value : getMessageString(messages, "", group, keySuffix);
    }
        
    private static final String getMessageString(Messages messages, String pfx, String group, String sfx) {
        String value = null;
        if ( StringUtils.isNotBlank(group) ) {
            value = messages.getString(pfx+group+"."+sfx, null);
        }
        return value!=null ? value : messages.getString(pfx+sfx, null);
    }
    
    public static final String getRequiredMessageString(CommandSpec commandSpec, String keySuffix) {
        String result = getMessageString(commandSpec, keySuffix);
        if ( StringUtils.isBlank(result) ) {
            throw new RuntimeException("No resource bundle entry found for required key suffix: "+keySuffix);
        }
        return result;
    }
    
    /**
     * @param commandSpec {@link CommandSpec} instance for looking up a {@link ResourceBundle}
     * @return {@link Messages} instance for the given {@link CommandSpec}, 
     *         or null if {@link CommandSpec} doesn't have a {@link ResourceBundle}
     */
    public static final Messages getMessages(CommandSpec commandSpec) {
        ResourceBundle resourceBundle = commandSpec.resourceBundle();
        return resourceBundle==null ? null : new Messages(commandSpec, resourceBundle);
    }
}
