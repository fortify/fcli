package com.fortify.cli.aviator.audit.model;
import com.fortify.cli.aviator.config.IAviatorLogger;

import com.fortify.cli.aviator.util.FprHandle;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AuditFprOptions {
    private final FprHandle fprHandle;
    private final String token;
    private final String url;
    private final String appVersion;
    private final String sscAppName;
    private final String sscAppVersion;
    private final IAviatorLogger logger;
    private final String tagMappingPath;
    private final String filterSetNameOrId;
    private final boolean noFilterSet;
    private final List<String> folderNames;
}