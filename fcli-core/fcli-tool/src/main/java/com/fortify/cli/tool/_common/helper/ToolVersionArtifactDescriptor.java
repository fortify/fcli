package com.fortify.cli.tool._common.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor 
@Data
public final class ToolVersionArtifactDescriptor {
    private String name;
    private String downloadUrl;
    private String rsa_sha256;
}
