/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.http.ssl.truststore.helper;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = false) @Builder
@Reflectable @NoArgsConstructor @AllArgsConstructor
public class TrustedUrlCertificateDescriptor extends JsonNodeHolder {
    private String key;
    private String url;
    private String host;
    private int port;
    private String sourceUrl;
    private String subject;
    private String issuer;
    private String serialNumber;
    private String sha256;
    private String notBefore;
    private String notAfter;
    private String certificatePem;
    private String createdAt;
}
