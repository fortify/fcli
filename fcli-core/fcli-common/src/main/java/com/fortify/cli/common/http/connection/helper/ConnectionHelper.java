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
package com.fortify.cli.common.http.connection.helper;

import java.nio.file.Path;
import java.util.stream.Stream;

import com.fortify.cli.common.util.FcliDataHelper;

import kong.unirest.UnirestInstance;

public final class ConnectionHelper {
    private ConnectionHelper() {}
    
    public static final void configureTimeouts(UnirestInstance unirest, String module) {
        getConnectTimeoutsStream()
            .filter(d->d.matchesModule(module))
            .findFirst()
            .ifPresent(d->
                unirest.config().connectTimeout(d.getTimeout())
            );
        getSocketTimeoutsStream()
        .filter(d->d.matchesModule(module))
        .findFirst()
        .ifPresent(d->
            unirest.config().socketTimeout(d.getTimeout())
        );
    }
    
    public static final TimeoutDescriptor getConnectTimeout(String name) {
        Path timeoutConfigPath = getConnectTimeoutConfigPath(name);
        if ( !FcliDataHelper.exists(timeoutConfigPath) ) {
            throw new IllegalArgumentException("No connect timeout configuration found with name: "+name);
        }
        return getTimeout(timeoutConfigPath);
    }
    
    public static final TimeoutDescriptor getSocketTimeout(String name) {
        Path timeoutConfigPath = getSocketTimeoutConfigPath(name);
        if ( !FcliDataHelper.exists(timeoutConfigPath) ) {
            throw new IllegalArgumentException("No socket timeout configuration found with name: "+name);
        }
        return getTimeout(timeoutConfigPath);
    }
    
    public static final TimeoutDescriptor addTimeout(TimeoutDescriptor descriptor) {
        Path timeoutConfigPath = getTimeoutConfigPath(descriptor);
        if ( FcliDataHelper.exists(timeoutConfigPath) ) {
            throw new IllegalArgumentException("timeout configuration with name "+descriptor.getName()+" already exists");
        }
        FcliDataHelper.saveSecuredFile(timeoutConfigPath, descriptor, true);
        return descriptor;
    }
    
    public static final TimeoutDescriptor updateTimeout(TimeoutDescriptor descriptor) {
        FcliDataHelper.saveSecuredFile(getTimeoutConfigPath(descriptor), descriptor, true);
        return descriptor;
    }
    
    private static final TimeoutDescriptor getTimeout(Path timeoutDescriptorPath) {
        return FcliDataHelper.readSecuredFile(timeoutDescriptorPath, TimeoutDescriptor.class, true);
    }
    
    public static final TimeoutDescriptor deleteTimeout(TimeoutDescriptor descriptor) {
        FcliDataHelper.deleteFile(getTimeoutConfigPath(descriptor), true);
        return descriptor;
    }
    
    public static final Stream<TimeoutDescriptor> deleteAllConnectTimeouts() {
        return getConnectTimeoutsStream()
                .peek(ConnectionHelper::deleteTimeout);
    }
    
    public static final Stream<TimeoutDescriptor> getConnectTimeoutsStream() {
        return FcliDataHelper.exists(getConnectTimeoutConfigPath())
                ? FcliDataHelper.listFilesInDir(getConnectTimeoutConfigPath(), true).map(ConnectionHelper::getTimeout)
                : Stream.empty();
    }
    
    public static final Stream<TimeoutDescriptor> deleteAllSocketTimeouts() {
        return getConnectTimeoutsStream()
                .peek(ConnectionHelper::deleteTimeout);
    }
    
    public static final Stream<TimeoutDescriptor> getSocketTimeoutsStream() {
        return FcliDataHelper.exists(getSocketTimeoutConfigPath())
                ? FcliDataHelper.listFilesInDir(getSocketTimeoutConfigPath(), true).map(ConnectionHelper::getTimeout)
                : Stream.empty();
    }
    
    private static final Path getConnectTimeoutConfigPath() {
        return FcliDataHelper.getFcliConfigPath().resolve("connecttimeouts");
    }
    
    private static final Path getConnectTimeoutConfigPath(String name) {
        return getConnectTimeoutConfigPath().resolve(getTimeoutFileName(name));
    }
    
    private static final Path getSocketTimeoutConfigPath() {
        return FcliDataHelper.getFcliConfigPath().resolve("sockettimeouts");
    }
    
    private static final Path getSocketTimeoutConfigPath(String name) {
        return getSocketTimeoutConfigPath().resolve(getTimeoutFileName(name));
    }
    
    private static final Path getTimeoutConfigPath(TimeoutDescriptor descriptor) {
        switch(descriptor.getType()) {
        case CONNECT:
            return getConnectTimeoutConfigPath().resolve(getTimeoutFileName(descriptor.getName()));
        case SOCKET:
            return getSocketTimeoutConfigPath().resolve(getTimeoutFileName(descriptor.getName()));
        default:
            throw new IllegalArgumentException("unknown timeoutdescriptor type");
        }
    }
    
    private static final String getTimeoutFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
