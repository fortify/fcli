package com.fortify.cli.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fortify.cli.common.util.IssueFilePathFinder.OnNoMatch;

import lombok.SneakyThrows;

/**
 *
 * @author Ruud Senden
 */
public class IssueFilePathFinderTest {
    private static final Path sampleRepoPath = createSampleRepo();
    private static final IssueFilePathFinder originalOnNoMatchFinder = IssueFilePathFinder.builder()
            .repoPath(sampleRepoPath)
            .onNoMatch(OnNoMatch.ORIGINAL)
            .build();
    private static final IssueFilePathFinder nullOnNoMatchFinder = IssueFilePathFinder.builder()
            .repoPath(sampleRepoPath)
            .onNoMatch(OnNoMatch.NULL)
            .build();
    private static final IssueFilePathFinder originalFinder = IssueFilePathFinder.builder()
            .repoPath(null)
            .onNoMatch(OnNoMatch.ORIGINAL)
            .build();
    private static final IssueFilePathFinder nullFinder = IssueFilePathFinder.builder()
            .repoPath(null)
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
    public void testOriginalOnMatchFinder(String fortifyPath, String expectedResult) throws Exception {
        test(originalOnNoMatchFinder, fortifyPath, expectedResult);
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
    public void testNullOnMatchFinder(String fortifyPath, String expectedResult) throws Exception {
        test(nullOnNoMatchFinder, fortifyPath, expectedResult);
    }
    
    @ParameterizedTest
    @CsvSource({
        "NonExistingRootTest.java,NonExistingRootTest.java",
        "src/main/java/NonExistingTest.java,src/main/java/NonExistingTest.java",
        "scancentral123/work/Test1.java,scancentral123/work/Test1.java"
        // TODO Determine expected behavior for absolute paths, and add test cases if necessary 
    })
    public void testOriginalFinder(String fortifyPath, String expectedResult) throws Exception {
        test(originalFinder, fortifyPath, expectedResult);
    }
    
    @ParameterizedTest
    @CsvSource({
        "NonExistingRootTest.java,",
        "src/main/java/NonExistingTest.java,",
        "scancentral123/work/Test1.java,"
        // TODO Determine expected behavior for absolute paths, and add test cases if necessary 
    })
    public void testNullFinder(String fortifyPath, String expectedResult) throws Exception {
        test(nullFinder, fortifyPath, expectedResult);
    }
    
    private void test(IssueFilePathFinder finder, String fortifyPath, String expectedResult) {
        expectedResult = StringUtils.isBlank(expectedResult) ? null : expectedResult;
        assertEquals(expectedResult, finder.find(fortifyPath));
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