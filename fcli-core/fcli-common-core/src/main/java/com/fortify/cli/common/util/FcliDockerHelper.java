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
package com.fortify.cli.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.Getter;

/**
 * Utility class for detecting Docker container environments and providing
 * Docker-specific help information.
 */
public final class FcliDockerHelper {
    private FcliDockerHelper() {}
    
    @Getter(lazy=true) private static final boolean runningInContainer = detectContainer();
    
    /**
     * Detects if fcli is running inside a container (Docker, Kubernetes, etc).
     * 
     * @return true if running in a container, false otherwise
     */
    private static final boolean detectContainer() {
        // Linux containers: Check for /.dockerenv file (created by Docker daemon)
        if (new File("/.dockerenv").exists()) {
            return true;
        }
        
        // Windows containers: Check for CONTAINER_SANDBOX_MOUNT_POINT environment variable
        if (System.getenv("CONTAINER_SANDBOX_MOUNT_POINT") != null) {
            return true;
        }
        
        // Fallback for Linux: Check /proc/1/cgroup for container runtimes
        try {
            if (Files.exists(Paths.get("/proc/1/cgroup"))) {
                return Files.lines(Paths.get("/proc/1/cgroup"))
                    .anyMatch(line -> line.contains("docker") || 
                                      line.contains("kubepods") || 
                                      line.contains("containerd"));
            }
        } catch (IOException e) {
            // Ignore - not a container or can't read cgroup
        }
        
        return false;
    }
    
    public static final String getDockerHelpNotice(int width) {
        if (!isRunningInContainer()) {
            return "";
        }
        
        String fcliUserHome = EnvHelper.getUserHome();
        String separator = "=".repeat(width);
        return String.format("""

            %s
            @|bold DOCKER CONTAINER DETECTED - State Persistence Notice|@

            fcli stores state data like sessions in: 
              @|bold %s|@

            Without a volume mount, state data and sessions are lost when the container exits. \
            For persistent state across 'docker run' commands, use volume mounts:
              docker run -v fcli-data:%s <image> <command>

            If using custom FCLI_* environment variables (FCLI_DATA_DIR, FCLI_CONFIG_DIR, FCLI_STATE_DIR), \
            you may need additional volume mounts.
            %s
            """, separator, fcliUserHome, fcliUserHome, separator);
    }
}
