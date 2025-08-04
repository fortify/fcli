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
import java.util.concurrent.Callable;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.output.cli.cmd.IOutputHelperSupplier;

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
    
    public static final String getMessageString(CommandSpec commandSpec, String keySuffix, Object... args) {
        var group = getCommandGroup(commandSpec);
        Messages messages = getMessages(commandSpec);
        String value = null;
        while ( commandSpec!=null && value==null ) {
            String pfx = commandSpec.qualifiedName(".")+".";
            value = getMessageString(messages, pfx, group, keySuffix);
            commandSpec = commandSpec.parent();
        }
        // If value is still null, try without any prefix
        value = value!=null ? value : getMessageString(messages, "", group, keySuffix);
        return formatMessage(value, args);
    }
        
    private static final String getMessageString(Messages messages, String pfx, String group, String sfx) {
        String value = null;
        if ( StringUtils.isNotBlank(group) ) {
            value = messages.getString(pfx+group+"."+sfx, null);
        }
        return value!=null ? value : messages.getString(pfx+sfx, null);
    }
    
    private static final String formatMessage(String msg, Object... args) {
        return msg==null || args==null || args.length==0 ? msg : String.format(msg, args);
    }
    
    public static final String getRequiredMessageString(CommandSpec commandSpec, String keySuffix, Object... args) {
        String result = getMessageString(commandSpec, keySuffix, args);
        if ( StringUtils.isBlank(result) ) {
            throw new FcliBugException("No resource bundle entry found for required key suffix: "+keySuffix);
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
    
    public static final boolean isHiddenSelf(CommandSpec spec) {
        return spec.usageMessage().hidden();
    }
    
    public static final boolean hasHiddenParent(CommandSpec spec) {
        var parent = spec.parent();
        if ( parent==null ) { return false; }
        if ( parent.usageMessage().hidden() ) { return true; }
        return hasHiddenParent(parent);
    }
    
    public static final boolean isHiddenSelfOrParent(CommandSpec spec) {
        return isHiddenSelf(spec) || hasHiddenParent(spec);
    }
    
    public static final boolean isRunnable(CommandSpec spec) {
        var userObject = userObject(spec);
        return userObject!=null && (userObject instanceof Runnable || userObject instanceof Callable<?>);
    }
    
    public static final Object userObject(CommandSpec spec) {
        return spec==null ? null : spec.userObject();
    }
    
    public static final boolean canCollectRecords(CommandSpec spec) {
        return JavaHelper.as(spec.userObject(), IOutputHelperSupplier.class).isPresent();
    }
}
