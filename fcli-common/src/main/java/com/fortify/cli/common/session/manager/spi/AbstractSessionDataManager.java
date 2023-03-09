/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.session.manager.spi;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.api.SessionSummary;
import com.fortify.cli.common.util.FcliHomeHelper;
import com.fortify.cli.common.util.FixInjection;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.SneakyThrows;

@ReflectiveAccess @FixInjection
public abstract class AbstractSessionDataManager<T extends ISessionData> implements ISessionDataManager<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionDataManager.class);
    @Getter @Inject private ObjectMapper objectMapper;
    
    @Override
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public final T get(String sessionName, boolean failIfUnavailable) {
        Path authSessionDataPath = getSessionDataPath(sessionName);
        checkSessionExists(sessionName, failIfUnavailable);
        try {
            String authSessionDataJson = FcliHomeHelper.readSecuredFile(authSessionDataPath, failIfUnavailable);
            T authSessionData = authSessionDataJson==null ? null : objectMapper.readValue(authSessionDataJson, getSessionDataType());
            checkNonExpiredSessionAvailable(sessionName, failIfUnavailable, authSessionData);
            return authSessionData;
        } catch ( Exception e ) {
            FcliHomeHelper.deleteFile(authSessionDataPath, false);
            conditionalThrow(failIfUnavailable, ()->new IllegalStateException("Error reading auth session data, please try logging in again", e));
            LOG.warn("Error reading session data from {}; session data has been deleted", authSessionDataPath);
            LOG.warn("Exception details: ", e);
            return null;
        }
    }

    @Override
    @SneakyThrows // TODO Do we want to use SneakyThrows? 
    public final void save(String sessionName, T sessionData) {
        String authSessionDataJson = objectMapper.writeValueAsString(sessionData);
        FcliHomeHelper.saveSecuredFile(getSessionDataPath(sessionName), authSessionDataJson, true);
    }
    
    @Override
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public final void destroy(String sessionName) {
        FcliHomeHelper.deleteFile(getSessionDataPath(sessionName), true);
    }
    
    @Override
    public final boolean exists(String sessionName) {
        return FcliHomeHelper.isReadable(getSessionDataPath(sessionName));
    }
    
    @Override
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public final List<String> sessionNames() {
        Path path = getSessionsDataPath();
        if ( !FcliHomeHelper.exists(path) ) {
            return Collections.emptyList();
        }
        return FcliHomeHelper.listFilesInDir(path, true)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }
    
    @Override
    public final Collection<SessionSummary> sessionSummaries() {
        return sessionNames().stream()
                .map(this::getSessionSummary)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public final ArrayNode sessionSummariesAsArrayNode() {
        return sessionSummaries().stream()
                .map(objectMapper::valueToTree)
                .map(JsonNode.class::cast) // TODO Not sure why this is necessary
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public ObjectNode sessionSummaryAsObjectNode(String sessionName) {
    	return objectMapper.valueToTree(getSessionSummary(sessionName));
    }
    
    private final Path getSessionsDataPath() {
        return FcliHomeHelper.getFcliStatePath().resolve("sessions").resolve(getSessionTypeName());
    }
    
    private final Path getSessionDataPath(String sessionName) {
        return getSessionsDataPath().resolve(sessionName);
    }
    
    private SessionSummary getSessionSummary(String sessionName) {
        T sessionData = get(sessionName, false);
        return sessionData==null ? null : 
            SessionSummary.builder()
                .name(sessionName)
                .type(getSessionTypeName())
                .url(sessionData.getUrlDescriptor())
                .created(sessionData.getCreatedDate())
                .expires(sessionData.getExpiryDate())
                .build();
    }
    
    private void conditionalThrow(boolean throwException, Supplier<RuntimeException> exceptionSupplier) {
        if ( throwException ) { throw exceptionSupplier.get(); }
    }

    private void checkNonExpiredSessionAvailable(String sessionName, boolean failIfUnavailable, T authSessionData) {
        if ( failIfUnavailable && isExpiredOrUnavailable(authSessionData) )
        {
            throw new IllegalStateException(getSessionTypeName()+" session '"+sessionName+"' cannot be retrieved or has expired, please login again");
        }
    }

    private void checkSessionExists(String sessionName, boolean failIfUnavailable) {
        if ( failIfUnavailable && !exists(sessionName) ) {
            throw new IllegalStateException(getSessionTypeName()+" session '"+sessionName+"' not found, please login first");
        }
    }
    
    private boolean isExpiredOrUnavailable(T authSessionData) {
        return authSessionData==null ||
                (authSessionData.getExpiryDate()!=null // We don't know whether session is expired if expiryDate is null
                && authSessionData.getExpiryDate().before(new Date()));
    }
    
    @Override
    public abstract String getSessionTypeName();
    
    protected abstract Class<T> getSessionDataType();
}
