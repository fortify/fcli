package com.fortify.cli.tool._common.helper;
import java.util.HashSet;
import java.util.Set;

public class ToolRegistry {
	private static final Set<String> REGISTERED_TOOLS = new HashSet<>();
	
	static {
		REGISTERED_TOOLS.add("bugtracker-utility");
		REGISTERED_TOOLS.add("debricked-cli");
		REGISTERED_TOOLS.add("fcli");
		REGISTERED_TOOLS.add("fod-uploader");
		REGISTERED_TOOLS.add("jre");
		REGISTERED_TOOLS.add("sc-client");
		REGISTERED_TOOLS.add("vuln-exporter");
	}

    public static Set<String> getRegisteredToolNames() {
        return REGISTERED_TOOLS;
    }
}
