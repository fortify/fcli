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

import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public abstract class AbstractActionStepProcessorMapEntries<K,V> extends AbstractActionStepProcessorEntries<Map.Entry<K,V>> {
    public final void process() {
        var map = getMap();
        if ( map!=null ) { map.entrySet().forEach(e->processEntry(e)); }
    }
    
    @Override
    protected final void process(Map.Entry<K, V> entry) {
        process(entry.getKey(), entry.getValue());
    }
    
    protected abstract void process(K key, V value);
    
    protected abstract Map<K,V> getMap();
}
