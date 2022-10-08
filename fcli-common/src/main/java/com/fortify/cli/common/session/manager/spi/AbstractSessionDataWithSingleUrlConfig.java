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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUrlConfigSupplier;
import com.fortify.cli.common.rest.runner.config.UrlConfig;
import com.fortify.cli.common.session.manager.api.ISessionData;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true) @ReflectiveAccess @JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractSessionDataWithSingleUrlConfig extends AbstractSessionData implements ISessionData, IUrlConfigSupplier {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig urlConfig;
    
    // No-arg constructor required for Jackson deserialization
    protected AbstractSessionDataWithSingleUrlConfig() {}
    
    public AbstractSessionDataWithSingleUrlConfig(IUrlConfig urlConfig) {
        this.urlConfig = urlConfig;
    }
    
    @Override
    public String getUrlDescriptor() {
        return urlConfig==null ? "Unknown" : urlConfig.getUrl();
    }
}
