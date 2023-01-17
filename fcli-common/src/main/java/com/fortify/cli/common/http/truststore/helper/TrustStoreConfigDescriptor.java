package com.fortify.cli.common.http.truststore.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = false) 
@Builder @NoArgsConstructor @AllArgsConstructor @ReflectiveAccess
public class TrustStoreConfigDescriptor extends JsonNodeHolder {
    private String path;
    private String type;
    private String password;
}
