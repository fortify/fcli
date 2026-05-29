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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for FileUtils glob/pattern matching functionality.
 */
class FileUtilsGlobTest {
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setupTestFiles() throws IOException {
        // Create test directory structure:
        // root/
        //   file1.txt
        //   file2.jar
        //   lib/
        //     tool-1.2.3.jar
        //     other.txt
        //   deep/
        //     nested/
        //       deep.jar
        
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.jar"));
        
        Path libDir = tempDir.resolve("lib");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("tool-1.2.3.jar"));
        Files.createFile(libDir.resolve("other.txt"));
        
        Path deepDir = tempDir.resolve("deep/nested");
        Files.createDirectories(deepDir);
        Files.createFile(deepDir.resolve("deep.jar"));
    }
    
    @Test
    void testSingleWildcardPattern() {
        // Match all JARs in root
        List<Path> results = FileUtils.processMatchingFileStream(
            tempDir, 
            "*.jar", 
            1,
            stream -> stream.collect(Collectors.toList())
        );
        
        assertEquals(1, results.size());
        assertTrue(results.get(0).getFileName().toString().equals("file2.jar"));
    }
    
    @Test
    void testPathWithSingleWildcard() {
        // Match all JARs in lib/
        List<Path> results = FileUtils.processMatchingFileStream(
            tempDir, 
            "lib/*.jar", 
            2,
            stream -> stream.collect(Collectors.toList())
        );
        
        assertEquals(1, results.size());
        assertTrue(results.get(0).getFileName().toString().equals("tool-1.2.3.jar"));
    }
    
    @Test
    void testDoubleWildcardPattern() {
        // Match all JARs recursively
        List<Path> results = FileUtils.processMatchingFileStream(
            tempDir, 
            "**/*.jar", 
            10,
            stream -> stream.collect(Collectors.toList())
        );
        
        assertEquals(3, results.size());
        List<String> fileNames = results.stream()
            .map(p -> p.getFileName().toString())
            .sorted()
            .collect(Collectors.toList());
        assertEquals(List.of("deep.jar", "file2.jar", "tool-1.2.3.jar"), fileNames);
    }
    
    @Test
    void testExactFilename() {
        // Match exact file in lib
        Path result = FileUtils.processMatchingFileStream(
            tempDir, 
            "lib/tool-1.2.3.jar", 
            2,
            stream -> stream.findFirst().orElse(null)
        );
        
        assertNotNull(result);
        assertEquals("tool-1.2.3.jar", result.getFileName().toString());
    }
    
    @Test
    void testNoMatch() {
        // Pattern that doesn't match anything
        Path result = FileUtils.processMatchingFileStream(
            tempDir, 
            "lib/*.zip", 
            2,
            stream -> stream.findFirst().orElse(null)
        );
        
        assertNull(result);
    }
    
    @Test
    void testDirectoryMatching() {
        // Match directories
        List<Path> results = FileUtils.processMatchingDirStream(
            tempDir, 
            "*", 
            1,
            stream -> stream.collect(Collectors.toList())
        );
        
        assertEquals(2, results.size());
        List<String> dirNames = results.stream()
            .map(p -> p.getFileName().toString())
            .sorted()
            .collect(Collectors.toList());
        assertEquals(List.of("deep", "lib"), dirNames);
    }
    
    @Test
    void testRecursiveDirectoryMatching() {
        // Match all directories recursively
        List<Path> results = FileUtils.processMatchingDirStream(
            tempDir, 
            "**/*", 
            10,
            stream -> stream.collect(Collectors.toList())
        );
        
        assertTrue(results.size() >= 3); // lib, deep, deep/nested
    }
}
