package com.fortify.cli.aviator.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;

@Data
@AllArgsConstructor
public class FPRAuditResult {
    private File updatedFile;
    private String status;
    private String message;
}