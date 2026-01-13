package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.detect-env")
class DetectEnvSpec extends FcliBaseSpec {
    
    def setup() {
        // Clear global action variables before each test to ensure clean state, as otherwise
        // no detection logic will be run after the first test due to global.isCiInitialized
        // already being true.
        // Use reflection since ActionRunnerVars is not in functional test classpath.
        try {
            def actionRunnerVarsClass = Class.forName("com.fortify.cli.common.action.runner.ActionRunnerVars")
            def globalValuesField = actionRunnerVarsClass.getDeclaredField("globalValues")
            globalValuesField.setAccessible(true)
            def globalValues = globalValuesField.get(null)
            globalValues.removeAll()
        } catch (ClassNotFoundException e) {
            // If class not found, we're probably running with ExternalRunner (not ReflectiveRunner)
            // In that case, each fcli invocation is a separate process with clean state anyway
        }
    }
    
    def "detect-env-github"() {
        def env = [
            "GITHUB_REPOSITORY": "owner/test-repo",
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_REF_NAME": "main",
            "GITHUB_SHA": "abc123def456abc123def456abc123def456abc1",
            "GITHUB_WORKSPACE": "/workspace",
            "GITHUB_STEP_SUMMARY": "/tmp/summary.md"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
            def allOutput = result.stdout + result.stderr
        then:
            // Check combined output (stdout + stderr) since detection messages may go to stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected GitHub") || it.contains("name: GitHub") }
                it.any { it.contains("id: github") }
                it.any { it.contains("qualifiedRepoName: owner/test-repo") }
                it.any { it.contains("sourceBranch: main") }
                it.any { it.contains("commitSHA: abc123def456abc123def456abc123def456abc1") }
                it.any { it.contains("sourceDir: /workspace") }
                it.any { it.contains("jobSummaryFile: /tmp/summary.md") }
            }
    }
    
    def "detect-env-gitlab"() {
        def env = [
            "GITLAB_CI": "true",
            "CI_PROJECT_ID": "12345",
            "CI_PROJECT_NAME": "myproject",
            "CI_PROJECT_PATH": "group/myproject",
            "CI_PROJECT_DIR": "/builds/project",
            "CI_COMMIT_SHA": "fedcba0987654321fedcba0987654321fedcba09",
            "CI_COMMIT_BRANCH": "develop",
            "CI_PIPELINE_ID": "9876"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected GitLab") || it.contains("name: GitLab") }
                it.any { it.contains("id: gitlab") }
                it.any { it.contains("qualifiedRepoName: group/myproject") }
                it.any { it.contains("sourceBranch: develop") }
                it.any { it.contains("commitSHA: fedcba0987654321fedcba0987654321fedcba09") }
                it.any { it.contains("sourceDir: /builds/project") }
            }
    }
    
    def "detect-env-ado"() {
        def env = [
            "Build.Repository.Name": "MyRepo",
            "Build.SourceBranch": "refs/heads/main",
            "Build.SourceBranchName": "main",
            "Build.SourceVersion": "9876543210abcdef9876543210abcdef98765432",
            "Build.SourcesDirectory": "/home/vsts/work/1/s",
            "System.TeamFoundationCollectionUri": "https://dev.azure.com/myorg/",
            "System.TeamProject": "MyProject"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected Azure DevOps") || it.contains("name: Azure DevOps") }
                it.any { it.contains("id: ado") }
                it.any { it.contains("qualifiedRepoName: MyRepo") }
                it.any { it.contains("sourceBranch: main") }
                it.any { it.contains("commitSHA: 9876543210abcdef9876543210abcdef98765432") }
                it.any { it.contains("sourceDir: /home/vsts/work/1/s") }
            }
    }

    def "detect-env-bitbucket"() {
        def env = [
            "BITBUCKET_WORKSPACE": "acme",
            "BITBUCKET_REPO_SLUG": "awesome-repo",
            "BITBUCKET_BRANCH": "main",
            "BITBUCKET_COMMIT": "11223344556677889900aabbccddeeff00112233",
            "BITBUCKET_CLONE_DIR": "/opt/build/source"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected Bitbucket") || it.contains("name: Bitbucket") }
                it.any { it.contains("id: bitbucket") }
                it.any { it.contains("qualifiedRepoName: acme/awesome-repo") }
                it.any { it.contains("sourceBranch: main") }
                it.any { it.contains("commitSHA: 11223344556677889900aabbccddeeff00112233") }
                it.any { it.contains("sourceDir: /opt/build/source") }
            }
    }
    
    def "detect-env-jenkins"() {
        def env = [
            "JENKINS_HOME": "/var/jenkins_home",
            "WORKSPACE": "/var/jenkins_home/workspace/my-job"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected Jenkins") || it.contains("name: Jenkins") }
                it.any { it.contains("id: jenkins") }
                it.any { it.contains("sourceDir: /var/jenkins_home/workspace/my-job") }
            }
    }
    
    def "detect-env-github-pr"() {
        def env = [
            "GITHUB_REPOSITORY": "owner/test-repo",
            "GITHUB_REF": "refs/pull/123/merge",
            "GITHUB_SHA": "def456abc123def456abc123def456abc123def4",
            "GITHUB_HEAD_REF": "feature-branch",
            "GITHUB_BASE_REF": "main",
            "GITHUB_WORKSPACE": "/workspace"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected GitHub") || it.contains("name: GitHub") }
                it.any { it.contains("sourceBranch: feature-branch") }
            }
    }
    
    def "detect-env-gitlab-mr"() {
        def env = [
            "GITLAB_CI": "true",
            "CI_PROJECT_ID": "12345",
            "CI_PROJECT_NAME": "myproject",
            "CI_PROJECT_PATH": "group/myproject",
            "CI_PROJECT_DIR": "/builds/project",
            "CI_COMMIT_SHA": "abc123",
            "CI_MERGE_REQUEST_IID": "42",
            "CI_MERGE_REQUEST_SOURCE_BRANCH_NAME": "feature-x",
            "CI_MERGE_REQUEST_TARGET_BRANCH_NAME": "main"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("Detected GitLab") || it.contains("name: GitLab") }
                it.any { it.contains("sourceBranch: feature-x") }
            }
    }
    
    def "detect-env-no-ci"() {
        when:
            def result = Fcli.run("action run detect-env", ciEnv([:]), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                // When running from a Git repository, it will detect "local Git repository"
                // Otherwise it will show "No CI system detected"
                it.any { it.contains("No CI system detected") || it.contains("local Git repository") }
            }
    }
    
    def "detect-env-custom-source-dir"() {
        def env = [
            "SOURCE_DIR": "/custom/source/path"
        ]
        when:
            def result = Fcli.run("action run detect-env", ciEnv(env), {it.expectZeroExitCode()})
        then:
            def allOutput = result.stdout + result.stderr
            verifyAll(allOutput) {
                it.any { it.contains("sourceDir: /custom/source/path") }
            }
    }

    private Map<String,String> ciEnv(Map<String,String> env) {
        def result = new LinkedHashMap<String,String>(blankCiEnv())
        env.each { k, v -> result.put(k, v == null ? "" : v.toString()) }
        return result
    }
    
    private static Map<String,String> blankCiEnv() {
        return new LinkedHashMap<String,String>(CI_ENV_BLANKS)
    }
    
    @Lazy
    private static final Map<String,String> CI_ENV_BLANKS = buildBlankCiEnv()
    
    private static Map<String,String> buildBlankCiEnv() {
        def envNames = new LinkedHashSet<String>(fetchAllCiEnvVarNames())
        def result = new LinkedHashMap<String,String>()
        envNames.each { result.put(it, "") }
        return Collections.unmodifiableMap(result)
    }

    private static Collection<String> fetchAllCiEnvVarNames() {
        try {
            def helperClass = Class.forName("com.fortify.cli.common.ci.CiEnvironmentTestHelper")
            def method = helperClass.getMethod("getAllCiEnvironmentVariableNames")
            def result = method.invoke(null)
            if (result instanceof Collection) {
                return result
            }
        } catch (ClassNotFoundException ignored) {
            // Continue with empty fallback if helper not accessible on classpath
        }
        return Collections.emptyList()
    }
}
