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
package com.fortify.cli.common.rest.unirest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;

public final class GenericUnirestFactory {
    private static final Logger LOG = LoggerFactory.getLogger(GenericUnirestFactory.class);
    private static final ConcurrentMap<String, UnirestInstance> instances = new ConcurrentHashMap<>();
    
    /**
     * Create a new {@link UnirestInstance}. Callers are responsible for closing the
     * {@link UnirestInstance} after use, for example using a try-with-resources block.
     * @return
     */
    public static final UnirestInstance createUnirestInstance() {
        UnirestInstance instance = Unirest.spawnInstance();
        instance.config().setObjectMapper(new JacksonObjectMapper(JsonHelper.getObjectMapper()));
        return instance;
    }
    
    /**
     * Get a {@link UnirestInstance} instance for the given key. If an instance
     * for the given key doesn't exist yet, a new instance will be created.
     * All previously created instances can be shut down by calling the {@link #shutdown()}
     * method, individual instances can be shut down by calling the {@link #shutdown(String)}
     * method.
     * @return
     */
    public static final UnirestInstance getUnirestInstance(String key) {
        UnirestInstance instance = instances.get(key);
        if ( instance==null ) {
            instance = createUnirestInstance();
            instances.put(key, instance);
        }
        return instance;
    }
    
    public static final void shutdown() {
        instances.keySet().stream().forEach(GenericUnirestFactory::shutdown);
    }
    
    public static final void shutdown(String key) {
        UnirestInstance instance = instances.remove(key);
        if ( instance!=null ) {
            try {
                instance.shutDown(true);
            } catch ( Exception e ) {
                String msg = "Error shutting down unirest instance"; 
                LOG.warn(msg);
                LOG.debug(msg, e);
            }
        }
    }
}
