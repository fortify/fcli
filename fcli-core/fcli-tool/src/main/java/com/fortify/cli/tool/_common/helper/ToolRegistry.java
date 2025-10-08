package com.fortify.cli.tool._common.helper;
import java.util.Set;

public class ToolRegistry {
	private static final Set<String> REGISTERED_TOOLS = registerTools();
	// TODO Whenever support for a given tool is added or removed, update this list
	// TODO Any way to have tools register themselves, to avoid this list becoming out of sync with tool commands?

    public static Set<String> getRegisteredToolNames() {
        return REGISTERED_TOOLS;
    }

    private static Set<String> registerTools() {
        return Set.of("bugtracker-utility", "debricked-cli", "fcli", "fod-uploader", "jre", "sc-client", "vuln-exporter");
    }
}