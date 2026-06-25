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
package com.fortify.cli.common.action.helper.git;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.util;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.credential.CredentialsProviderFactory;
import com.fortify.cli.common.ci.CiBranch;
import com.fortify.cli.common.ci.CiCommit;
import com.fortify.cli.common.ci.CiCommitId;
import com.fortify.cli.common.ci.CiCommitMessage;
import com.fortify.cli.common.ci.CiPerson;
import com.fortify.cli.common.ci.CiRepository;
import com.fortify.cli.common.ci.CiRepositoryName;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.util.EnvHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * SpEL functions for performing Git operations on a local repository.
 * Provides functionality for checking working tree status, creating branches,
 * staging files, committing, and pushing changes to a remote.
 *
 * Available via the {@code #git} SpEL variable in action YAML files.
 *
 * @author Sangamesh Vijayakumar
 */
@Reflectable
@SpelFunctionPrefix("git.")
@Slf4j
public class ActionGitSpelFunctions {
    public static final ActionGitSpelFunctions INSTANCE = new ActionGitSpelFunctions();

    @SpelFunction(cat = util, desc = """
            Returns basic information about the local git repository for the given source directory, or null if the
            directory is not inside a git working tree. Only constant-time lookups are performed (HEAD commit only).
            Structure:
            {
              repository: { workspaceDir, remoteUrl?, name: { short, full? } },
              branch: { full?, short? },
              commit: {
                id: { full, short },
                message: { short, full },
                author: { name, email, when },
                committer: { name, email, when }
              }
            }
            """, returns = "Git repository information or null if not a git work dir")
    public ObjectNode localRepo(
            @SpelFunctionParam(name = "sourceDir", desc = "directory assumed to be inside a git working tree") String sourceDir) {
        if (StringUtils.isBlank(sourceDir)) {
            return null;
        }
        var dir = Path.of(sourceDir).toAbsolutePath().normalize().toFile();
        if (!dir.exists()) {
            return null;
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(dir);
        if (builder.getGitDir() == null) {
            return null;
        }
        try (Repository repo = builder.build()) {
            var mapper = JsonHelper.getObjectMapper();
            var remote = selectRemote(repo);
            var remoteUrl = remote == null ? "origin" : repo.getConfig().getString("remote", remote, "url");
            var names = deriveRepoNames(dir.getName(), remoteUrl);
            var repository = CiRepository.builder()
                    .workspaceDir(repo.getWorkTree().getAbsolutePath())
                    .remoteUrl(StringUtils.isBlank(remoteUrl) ? null : remoteUrl)
                    .name(CiRepositoryName.builder()
                            .short_(names[0])
                            .full(names[1])
                            .build())
                    .build();

            CiBranch branch = null;
            try {
                String fullBranch = repo.getFullBranch();
                if (fullBranch != null) {
                    branch = CiBranch.builder()
                            .full(fullBranch)
                            .short_(Repository.shortenRefName(fullBranch))
                            .build();
                }
            } catch (Exception e) {
                /* ignore */ }

            CiCommit commit = null;
            var headId = repo.resolve("HEAD");
            if (headId != null) {
                try (var walk = new org.eclipse.jgit.revwalk.RevWalk(repo)) {
                    var gitCommit = walk.parseCommit(headId);
                    String shortId;
                    try {
                        var abbrev = repo.newObjectReader().abbreviate(gitCommit.getId(), 8);
                        shortId = abbrev.name();
                    } catch (Exception ex) {
                        shortId = gitCommit.getId().getName().substring(0, 8);
                    }

                    var authorIdent = gitCommit.getAuthorIdent();
                    var committerIdent = gitCommit.getCommitterIdent();

                    var commitId = CiCommitId.builder()
                            .full(gitCommit.getId().getName())
                            .short_(shortId)
                            .build();

                    commit = CiCommit.builder()
                            .headId(commitId)
                            .mergeId(commitId)
                            .message(CiCommitMessage.builder()
                                    .short_(gitCommit.getShortMessage())
                                    .full(gitCommit.getFullMessage())
                                    .build())
                            .author(authorIdent != null ? CiPerson.builder()
                                    .name(authorIdent.getName())
                                    .email(authorIdent.getEmailAddress())
                                    .when(authorIdent.getWhenAsInstant().toString())
                                    .build() : null)
                            .committer(committerIdent != null ? CiPerson.builder()
                                    .name(committerIdent.getName())
                                    .email(committerIdent.getEmailAddress())
                                    .when(committerIdent.getWhenAsInstant().toString())
                                    .build() : null)
                            .build();
                } catch (Exception e) {
                    /* ignore */ }
            }

            var root = mapper.createObjectNode();
            root.set("repository", mapper.valueToTree(repository));
            if (branch != null) {
                root.set("branch", mapper.valueToTree(branch));
            }
            if (commit != null) {
                root.set("commit", mapper.valueToTree(commit));
            }
            return root;
        } catch (Exception e) {
            return null;
        }
    }

    @SpelFunction(cat = util, desc = "Checks whether the working tree of the git repository at the given directory has any uncommitted changes (modified, added, or deleted files).", returns = "`true` if there are uncommitted changes, `false` otherwise or if not a git repository")
    public boolean hasChanges(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                return false;
            }
            var status = git.status().call();
            boolean hasChanges = !status.getModified().isEmpty()
                    || !status.getAdded().isEmpty()
                    || !status.getRemoved().isEmpty()
                    || !status.getUntracked().isEmpty()
                    || !status.getChanged().isEmpty();
            return hasChanges;
        } catch (Exception e) {
            return false;
        }
    }

    @SpelFunction(cat = util, desc = "Creates a new branch with the given full name in the local git repository and checks it out.", returns = "The name of the created branch")
    public String checkoutNewBranch(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name = "branchName", desc = "full branch name to create and checkout") String branchName) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .call();
            String current = git.getRepository().getBranch();
            if (!branchName.equals(current)) {
                throw new FcliSimpleException("Failed to checkout branch " + branchName);
            }
            return branchName;
        } catch (GitAPIException | IOException e) {
            throw new FcliSimpleException("Failed to create branch: " + e.getMessage());
        }
    }

    @SpelFunction(cat = util, desc = "Stages all modified and new files in the working tree for commit.", returns = "`true` if files were staged successfully")
    public boolean addAll(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            git.add().setUpdate(true).addFilepattern(".").call();
            return true;
        } catch (GitAPIException e) {
            throw new FcliSimpleException("Failed to stage files: " + e.getMessage());
        }
    }

    @SpelFunction(cat = util, desc = "Commits all staged changes in the local git repository with the given message.", returns = "The commit SHA of the new commit")
    public String commit(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name = "message", desc = "commit message") String message,
            @SpelFunctionParam(name = "name", desc = "commit author name") String name,
            @SpelFunctionParam(name = "email", desc = "commit author email") String email) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }

            if (git.status().call().isClean()) {
                throw new FcliSimpleException("No changes to commit");
            }

            var commitResult = git.commit()
                    .setMessage(message)
                    .setAuthor(name, email)
                    .setCommitter(name, email)
                    .call();
            var sha = commitResult.getId().getName();
            return sha;
        } catch (GitAPIException e) {
            throw new FcliSimpleException("Failed to commit: " + e.getMessage());
        }
    }

    @SpelFunction(cat = util, desc = "Pushes the current branch to the remote repository. Uses token-based authentication from CI environment variables (GITHUB_TOKEN, CI_JOB_TOKEN, SYSTEM_ACCESSTOKEN, BITBUCKET_TOKEN) if available.", returns = "The name of the remote ref that was pushed")
    public String push(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name = "branchName", desc = "name of the branch to push") String branchName) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            var repo = git.getRepository();
            var remote = selectRemote(repo);
            if (remote == null)
                remote = "origin";
            try {
                git.checkout().setName(branchName).call();
            } catch (Exception e) {
                git.checkout()
                        .setCreateBranch(true)
                        .setName(branchName)
                        .setStartPoint("HEAD")
                        .call();
            }

            var remoteUrl = repo.getConfig().getString("remote", remote, "url");
            if (remoteUrl != null && !remoteUrl.endsWith(".git")) {
                remoteUrl = remoteUrl + ".git";
                repo.getConfig().setString("remote", remote, "url", remoteUrl);
                repo.getConfig().save();
            }
            var credentialsProvider = CredentialsProviderFactory.detectAndGetJGitProvider();
            if (credentialsProvider == null) {
                log.debug("PUSH DEBUG: No credentials provider detected - push will likely fail");
            } else {
                log.debug("PUSH DEBUG: Using credentials provider={}", credentialsProvider.getClass().getName());
            }

            String fullBranchRef = "refs/heads/" + branchName;
            log.debug("PUSH DETAILS: branch={}, remote={}, remoteUrl={}, fullBranchRef={}",
                    branchName,
                    remote,
                    remoteUrl,
                    fullBranchRef);
            var refSpec = new RefSpec(fullBranchRef + ":" + fullBranchRef);
            if (credentialsProvider != null) {
                log.debug("CREDENTIALS: type={}, class={}",
                        credentialsProvider.getClass().getSimpleName(),
                        credentialsProvider.getClass().getName());
            }

            log.debug("PUSH COMMAND SETUP: remote={}, refSpec={}, timeout=300s, credentialsSet={}",
                    remote,
                    refSpec.toString(),
                    credentialsProvider != null);
            var fetchCmd = git.fetch().setRemote(remote);
            if (credentialsProvider != null) {
                fetchCmd.setCredentialsProvider(credentialsProvider);
            }

            try {
                var fetchResult = fetchCmd.call();
                log.debug("Fetch completed with {} ref updates",
                        fetchResult != null ? fetchResult.getAdvertisedRefs().size() : 0);
            } catch (Exception e) {
                log.warn("Fetch failed (but continuing with push): {}", e.getMessage(), e);
            }

            var pushCmd = git.push()
                    .setRemote(remote)
                    .setRefSpecs(refSpec)
                    .setTimeout(300);

            if (credentialsProvider != null) {
                pushCmd.setCredentialsProvider(credentialsProvider);
            }
            var results = pushCmd.call();

            log.debug("Push command completed. credentialsProvider: {}",
                    credentialsProvider != null ? credentialsProvider.getClass().getSimpleName() : "null");

            // Don't convert to ArrayList - just iterate directly
            // We can't use .isEmpty() or .size() on Iterable, so remove those checks

            StoredConfig config = repo.getConfig();
            config.setString("branch", branchName, "remote", remote);
            config.setString("branch", branchName, "merge", fullBranchRef);
            config.save();

            boolean success = false;
            boolean hasResults = false;
            for (var result : results) {
                hasResults = true;
                var messages = result.getMessages();
                if (messages != null && !messages.isBlank()) {
                    log.debug("Push result messages: {}", messages);
                }

                for (var update : result.getRemoteUpdates()) {
                    var status = update.getStatus();
                    log.debug("Push update: status={}, remoteName={}, message='{}', forceUpdate={}",
                            status,
                            update.getRemoteName(),
                            update.getMessage() != null ? update.getMessage() : "null",
                            update.isForceUpdate());
                    switch (status) {
                        case OK:
                        case UP_TO_DATE:
                            success = true;
                            log.debug("Push successful: {}", update.getRemoteName());
                            break;

                        case REJECTED_NONFASTFORWARD:
                        case REJECTED_NODELETE:
                        case REJECTED_REMOTE_CHANGED:
                        case REJECTED_OTHER_REASON:
                        case NON_EXISTING:
                        case NOT_ATTEMPTED:
                        default:
                            throw new FcliSimpleException(
                                    "Push rejected: "
                                            + "status=" + status
                                            + ", remote=" + update.getRemoteName()
                                            + ", message="
                                            + (update.getMessage() != null ? update.getMessage() : "no message"));
                    }
                }
            }

            if (!hasResults) {
                log.warn("Push returned empty results - push may have failed silently");
                throw new FcliSimpleException("Push completed but returned no results");
            }

            if (!success) {
                throw new FcliSimpleException(
                        "Push completed but no refs were updated (likely auth or permission issue)");
            }
            return fullBranchRef;
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            throw new FcliSimpleException(
                    "Failed to push (root cause): " + root.getClass().getName() + " - " + root.getMessage(),
                    e);
        }
    }

    @SpelFunction(cat = util, desc = "Detects the repository owner from CI environment variables. Checks GITHUB_REPOSITORY_OWNER (GitHub), CI_PROJECT_NAMESPACE (GitLab), BUILD_REPOSITORY_ID (Azure DevOps), or BITBUCKET_WORKSPACE (Bitbucket). Returns null if not running in a supported CI system.", returns = "The repository owner/namespace or null if not detectable")
    public String ciRepositoryOwner() {
        var owner = EnvHelper.env("GITHUB_REPOSITORY_OWNER");
        if (StringUtils.isNotBlank(owner)) {
            return owner;
        }
        owner = EnvHelper.env("CI_PROJECT_NAMESPACE");
        if (StringUtils.isNotBlank(owner)) {
            return owner;
        }
        var buildRepoId = EnvHelper.env("BUILD_REPOSITORY_ID");
        owner = EnvHelper.env("SYSTEM_TEAMPROJECT");
        if (StringUtils.isNotBlank(buildRepoId) && StringUtils.isNotBlank(owner)) {
            return owner;
        }
        owner = EnvHelper.env("BITBUCKET_WORKSPACE");
        if (StringUtils.isNotBlank(owner)) {
            return owner;
        }
        return null;
    }

    @SpelFunction(cat = util, desc = "Detects the default branch of the remote repository. Checks CI environment variables (CI_DEFAULT_BRANCH for GitLab, looks up via GitHub API env), then falls back to reading refs/remotes/origin/HEAD from the local git config. Returns null if detection fails.", returns = "The default branch name (e.g. 'main', 'master', 'develop') or null if not detectable")
    public String defaultBranch(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir) {
        var defaultBranch = EnvHelper.env("CI_DEFAULT_BRANCH");
        if (StringUtils.isNotBlank(defaultBranch)) {
            return defaultBranch;
        }
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                return null;
            }
            var repo = git.getRepository();
            var remoteHead = repo.resolve("refs/remotes/origin/HEAD");
            if (remoteHead != null) {
                var ref = repo.exactRef("refs/remotes/origin/HEAD");
                if (ref != null && ref.getTarget() != null) {
                    var target = ref.getTarget().getName();
                    if (target.startsWith("refs/remotes/origin/")) {
                        return target.substring("refs/remotes/origin/".length());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error detecting default branch", e);
        }
        return null;
    }

    @SpelFunction(cat = util, desc = """
            Detects the hosting platform of the repository by parsing the git remote URL.
            Returns "github" for GitHub-hosted repositories (github.com or *.github.com),
            "gitlab" for GitLab-hosted repositories (gitlab.com or hostnames containing "gitlab"),
            and "unknown" for any other remote or when detection fails.
            This is platform detection (where the repo lives), not CI detection (where the pipeline runs).
            """, returns = "\"github\", \"gitlab\", or \"unknown\"")
    public String repositoryPlatform(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                return "unknown";
            }
            var repo = git.getRepository();
            var remote = selectRemote(repo);
            if (remote == null) {
                return "unknown";
            }
            var remoteUrl = repo.getConfig().getString("remote", remote, "url");
            return detectPlatformFromUrl(remoteUrl);
        } catch (Exception e) {
            log.debug("Failed to detect repository platform", e);
            return "unknown";
        }
    }

    private Git openGit(String sourceDir) {
        if (StringUtils.isBlank(sourceDir)) {
            return null;
        }
        try {
            var dir = Path.of(sourceDir).toAbsolutePath().normalize().toFile();
            if (!dir.exists()) {
                return null;
            }
            var builder = new FileRepositoryBuilder().findGitDir(dir);
            if (builder.getGitDir() == null) {
                return null;
            }
            return new Git(builder.build());
        } catch (Exception e) {
            return null;
        }
    }

    private static String selectRemote(Repository repo) {
        try {
            var remotes = repo.getRemoteNames();
            if (remotes == null || remotes.isEmpty())
                return null;
            return remotes.contains("origin") ? "origin" : remotes.iterator().next();
        } catch (Exception e) {
            return null;
        }
    }

    private static String[] deriveRepoNames(String fallbackShort, String remoteUrl) {
        if (StringUtils.isBlank(remoteUrl)) {
            return new String[] { fallbackShort, null };
        }
        try {
            var cleaned = remoteUrl.trim();
            if (cleaned.endsWith(".git")) {
                cleaned = cleaned.substring(0, cleaned.length() - 4);
            }
            String pathPart;
            if (cleaned.startsWith("git@")) {
                int idx = cleaned.indexOf(":");
                pathPart = idx >= 0 ? cleaned.substring(idx + 1) : cleaned;
            } else {
                var uri = java.net.URI.create(cleaned);
                pathPart = uri.getPath();
                if (pathPart.startsWith("/")) {
                    pathPart = pathPart.substring(1);
                }
            }
            var parts = pathPart.split("/");
            if (parts.length >= 2) {
                var shortName = parts[parts.length - 1];
                return new String[] { shortName, pathPart };
            }
            return new String[] { parts[parts.length - 1], null };
        } catch (Exception e) {
            return new String[] { fallbackShort, null };
        }
    }

    private static String detectPlatformFromUrl(String remoteUrl) {
        if (StringUtils.isBlank(remoteUrl)) {
            return "unknown";
        }
        try {
            String host;
            var cleaned = remoteUrl.trim();
            if (cleaned.startsWith("git@")) {
                // SSH: git@github.com:owner/repo.git
                int colon = cleaned.indexOf(':');
                int at = cleaned.indexOf('@');
                host = (at >= 0 && colon > at) ? cleaned.substring(at + 1, colon) : null;
            } else {
                host = java.net.URI.create(cleaned).getHost();
            }
            if (host == null) {
                return "unknown";
            }
            host = host.toLowerCase();
            if (host.equals("github.com") || host.endsWith(".github.com")) {
                return "github";
            }
            if (host.equals("gitlab.com") || host.contains("gitlab")) {
                return "gitlab";
            }
        } catch (Exception e) {
            log.debug("Failed to parse remote URL for platform detection: {}", remoteUrl);
        }
        return "unknown";
    }
}
