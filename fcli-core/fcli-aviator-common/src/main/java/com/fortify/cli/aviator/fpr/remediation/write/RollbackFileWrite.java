package com.fortify.cli.aviator.fpr.remediation.write;

import java.nio.file.Path;

public record RollbackFileWrite(String filename, Path filePath, byte[] originalBytes) {
}
