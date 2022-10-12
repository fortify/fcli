package com.fortify.cli.common.util;

import java.lang.annotation.Annotation;
import java.util.ResourceBundle;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.Messages;

public class CommandSpecHelper {
    public static final <T extends Annotation> T findAnnotation(CommandSpec commandSpec, Class<T> annotationType) {
        T annotation = null;
        while ( commandSpec!=null && annotation==null ) {
            Object cmd = commandSpec.userObject();
            annotation = cmd==null ? null : cmd.getClass().getAnnotation(annotationType);
            commandSpec = commandSpec.parent();
        }
        return annotation;
    }
    
    public static final String getMessageString(CommandSpec commandSpec, String keySuffix) {
        Messages messages = getMessages(commandSpec);
        String value = null;
        while ( commandSpec!=null && value==null ) {
            String key = commandSpec.qualifiedName(".")+"."+keySuffix;
            value = messages.getString(key, null);
            commandSpec = commandSpec.parent();
        }
        // If value is still null, try without any prefix
        return value!=null ? value : messages.getString(keySuffix, null);
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
