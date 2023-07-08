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
package com.fortify.cli.common.session.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUrlConfigSupplier;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true) 
@Reflectable @NoArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractSessionDescriptorWithSingleUrlConfig extends AbstractSessionDescriptor implements ISessionDescriptor, IUrlConfigSupplier {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig urlConfig;
    
    public AbstractSessionDescriptorWithSingleUrlConfig(IUrlConfig urlConfig) {
        this.urlConfig = urlConfig;
    }
    
    @Override
    public String getUrlDescriptor() {
        return urlConfig==null ? "Unknown" : urlConfig.getUrl();
    }
}
