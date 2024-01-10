package com.fortify.cli.tool._common.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor 
@Data
public final class ToolVersionArtifactDescriptor {
    private String name;
    private String downloadUrl;
    private String rsa_sha256;
}
