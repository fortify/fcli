package com.fortify.cli.aviator.util;

import java.util.HashMap;
import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.NoArgsConstructor;

@NoArgsConstructor @Reflectable
public class ExtensionsConfig {
    private Map<String, String> supportedExtensions = new HashMap<>();

    public void setSupportedExtensions(Map<String, String> extensions) {
        this.supportedExtensions = extensions != null ? new HashMap<>(extensions) : new HashMap<>();
    }

    public Map<String, String> getExtensions() {
        return new HashMap<>(supportedExtensions);
    }

    public String getLanguageForExtension(String extension) {
        if (StringUtil.isEmpty(extension)) {
            return "Unknown";
        }
        var ext = extension.startsWith(".")
                ? extension
                : "." + extension;

        return supportedExtensions.getOrDefault(ext, "Unknown");
    }
}
