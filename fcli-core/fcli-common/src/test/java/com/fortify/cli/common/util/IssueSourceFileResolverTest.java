/*
 * Copyright 2021-2025 Open Text.
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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fortify.cli.common.util.IssueSourceFileResolver.OnNoMatch;

import lombok.SneakyThrows;

/**
 *
 * @author Ruud Senden
 */
public class IssueSourceFileResolverTest {
    private static final Path sampleSourcePath = createSampleRepo();
    private static final IssueSourceFileResolver originalOnNoMatchResolver = IssueSourceFileResolver.builder()
            .sourcePath(sampleSourcePath)
            .onNoMatch(OnNoMatch.ORIGINAL)
            .build();
    private static final IssueSourceFileResolver nullOnNoMatchResolver = IssueSourceFileResolver.builder()
            .sourcePath(sampleSourcePath)
            .onNoMatch(OnNoMatch.NULL)
            .build();
    private static final IssueSourceFileResolver originalResolver = IssueSourceFileResolver.builder()
            .sourcePath(null)
            .onNoMatch(OnNoMatch.ORIGINAL)
            .build();
    private static final IssueSourceFileResolver nullResolver = IssueSourceFileResolver.builder()
            .sourcePath(null)
            .onNoMatch(OnNoMatch.NULL)
            .build();
    
    @ParameterizedTest
    @CsvSource({
        "NonExistingRootTest.java,NonExistingRootTest.java",
        "src/main/java/NonExistingTest.java,src/main/java/NonExistingTest.java",
        "scancentral123/work/Test1.java,scancentral123/work/Test1.java",
        "RootTest.java,RootTest.java",
        "scancentral123/work/RootTest.java,RootTest.java",
        "src/main/java/com/fortify/Test1.java,src/main/java/com/fortify/Test1.java",
        "com/fortify/Test1.java,src/main/java/com/fortify/Test1.java",
        "Test1.java,src/main/java/com/fortify/Test1.java",
        "scancentral123/work/src/main/java/com/fortify/Test1.java,src/main/java/com/fortify/Test1.java"
        // TODO Determine expected behavior for absolute paths, and add test cases if necessary 
    })
    public void testOriginalOnMatchResolver(String fortifyPath, String expectedResult) throws Exception {
        test(originalOnNoMatchResolver, fortifyPath, expectedResult);
    }
    
    @ParameterizedTest
    @CsvSource({
        "NonExistingRootTest.java,",
        "src/main/java/NonExistingTest.java,",
        "scancentral123/work/Test1.java,",
        "RootTest.java,RootTest.java",
        "scancentral123/work/RootTest.java,RootTest.java",
        "src/main/java/com/fortify/Test1.java,src/main/java/com/fortify/Test1.java",
        "com/fortify/Test1.java,src/main/java/com/fortify/Test1.java",
        "Test1.java,src/main/java/com/fortify/Test1.java",
        "scancentral123/work/src/main/java/com/fortify/Test1.java,src/main/java/com/fortify/Test1.java"
        // TODO Determine expected behavior for absolute paths, and add test cases if necessary 
    })
    public void testNullOnMatchResolver(String fortifyPath, String expectedResult) throws Exception {
        test(nullOnNoMatchResolver, fortifyPath, expectedResult);
    }
    
    @ParameterizedTest
    @CsvSource({
        "NonExistingRootTest.java,NonExistingRootTest.java",
        "src/main/java/NonExistingTest.java,src/main/java/NonExistingTest.java",
        "scancentral123/work/Test1.java,scancentral123/work/Test1.java"
        // TODO Determine expected behavior for absolute paths, and add test cases if necessary 
    })
    public void testOriginalResolver(String fortifyPath, String expectedResult) throws Exception {
        test(originalResolver, fortifyPath, expectedResult);
    }
    
    @ParameterizedTest
    @CsvSource({
        "NonExistingRootTest.java,",
        "src/main/java/NonExistingTest.java,",
        "scancentral123/work/Test1.java,"
        // TODO Determine expected behavior for absolute paths, and add test cases if necessary 
    })
    public void testNullResolver(String fortifyPath, String expectedResult) throws Exception {
        test(nullResolver, fortifyPath, expectedResult);
    }
    
    private void test(IssueSourceFileResolver resolver, String fortifyPath, String expectedResult) {
        expectedResult = StringUtils.isBlank(expectedResult) ? null : expectedResult;
        assertEquals(expectedResult, resolver.resolve(fortifyPath));
    }

    @SneakyThrows
    private static final Path createSampleRepo() {
        var sampleRepoPath = Files.createTempDirectory("IssueFilePathTestSampleRepo");
        Runtime.getRuntime().addShutdownHook(new Thread(()->FileUtils.deleteRecursive(sampleRepoPath)));
        Files.createDirectories(sampleRepoPath.resolve("src/main/java/com/fortify/"));
        Files.createFile(sampleRepoPath.resolve("src/main/java/com/fortify/Test1.java"));
        Files.createFile(sampleRepoPath.resolve("src/main/java/com/fortify/Test2.java"));
        Files.createFile(sampleRepoPath.resolve("RootTest.java"));
        return sampleRepoPath;
    }
}