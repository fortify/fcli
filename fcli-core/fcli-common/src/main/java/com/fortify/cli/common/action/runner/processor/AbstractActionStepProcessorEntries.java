/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.runner.processor;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.FcliActionStepException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public abstract class AbstractActionStepProcessorEntries<T> extends AbstractActionStepProcessor {
    protected final void processEntry(T entry) {
        if ( _if(entry) ) {
            var entryStringIfDebugEnabled = getEntryAsStringIfDebugEnabled(entry);
            logDebug("Start processing", entryStringIfDebugEnabled, null);
            try {
                process(entry);
                logDebug("End processing", entryStringIfDebugEnabled, null);
            } catch ( Exception e ) {
                logDebug("Error processing", entryStringIfDebugEnabled, e);
                throw formatException(e, entryStringIfDebugEnabled, entry);
            }
        }
    }
    protected abstract void process(T entry);

    private void logDebug(String header, String detail, Exception e) {
        if ( detail!=null ) {
            LOG.debug(formatMessage(header, detail), e);
        }
    }

    private String getEntryAsStringIfDebugEnabled(T entry) {
        return LOG.isDebugEnabled() ? getEntryAsString(entry) : null;
    }
    
    private String formatMessage(String prefix, T entry) {
        return formatMessage(prefix, getEntryAsString(entry));
    }
    
    private String formatMessage(String header, String detail) {
        return String.format("%s:\n  %s", header, detail);
    }

    private FcliActionStepException formatException(Exception e, String optionalEntryString, T entry) {
        if ( e instanceof FcliActionStepException ) {
            return (FcliActionStepException)e;
        } else {
            var header = "Error processing";
            var msg = optionalEntryString==null ? formatMessage(header, entry) : formatMessage(header, optionalEntryString);
            return new FcliActionStepException(msg, e);
        }
    }
}
