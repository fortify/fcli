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
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
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

    @SpelFunction(cat=util, desc="""
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
            """, returns="Git repository information or null if not a git work dir")
    public ObjectNode localRepo(
            @SpelFunctionParam(name="sourceDir", desc="directory assumed to be inside a git working tree") String sourceDir) {
        if (StringUtils.isBlank(sourceDir)) { return null; }
        var dir = Path.of(sourceDir).toAbsolutePath().normalize().toFile();
        if (!dir.exists()) { return null; }
        FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(dir);
        if (builder.getGitDir() == null) { return null; }
        try (Repository repo = builder.build()) {
            log.debug("localRepo: Processing sourceDir={}", sourceDir);
            var mapper = JsonHelper.getObjectMapper();

            var remote = selectRemote(repo);
            var remoteUrl = remote == null ? null : repo.getConfig().getString("remote", remote, "url");
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
            } catch (Exception e) { /* ignore */ }

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
                } catch (Exception e) { /* ignore */ }
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
        } catch (Exception e) { return null; }
    }

    @SpelFunction(cat=util, desc="Checks whether the working tree of the git repository at the given directory has any uncommitted changes (modified, added, or deleted files).",
            returns="`true` if there are uncommitted changes, `false` otherwise or if not a git repository")
    public boolean hasChanges(
            @SpelFunctionParam(name="sourceDir", desc="directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) { return false; }
            log.debug("hasChanges: Checking for uncommitted changes in sourceDir={}", sourceDir);
            var status = git.status().call();
            return !status.getModified().isEmpty()
                || !status.getAdded().isEmpty()
                || !status.getRemoved().isEmpty()
                || !status.getUntracked().isEmpty()
                || !status.getChanged().isEmpty();
        } catch (Exception e) {
            log.debug("Error checking git status", e);
            return false;
        }
    }

    @SpelFunction(cat=util, desc="Creates a new branch in the local git repository and checks it out. The branch name is based on the provided prefix and a timestamp suffix to ensure uniqueness (e.g., 'fcli/remediation/20260520-103045').",
            returns="The name of the created branch")
    public String createBranch(
            @SpelFunctionParam(name="sourceDir", desc="directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name="branchPrefix", desc="prefix for the branch name (e.g., 'fcli/remediation')") String branchPrefix) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            var timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            var branchName = branchPrefix + "/" + timestamp;
            git.checkout().setCreateBranch(true).setName(branchName).call();
            log.debug("createBranch: Created branch={}", branchName);
            log.info("Created and checked out branch: {}", branchName);
            return branchName;
        } catch (GitAPIException e) {
            throw new FcliSimpleException("Failed to create branch: " + e.getMessage());
        }
    }

    @SpelFunction(cat=util, desc="Stages all modified and new files in the working tree for commit.",
            returns="`true` if files were staged successfully")
    public boolean addAll(
            @SpelFunctionParam(name="sourceDir", desc="directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            log.debug("addAll: Staged all changes in sourceDir={}", sourceDir);
            log.info("Staged all changes in: {}", sourceDir);
            return true;
        } catch (GitAPIException e) {
            throw new FcliSimpleException("Failed to stage files: " + e.getMessage());
        }
    }

    @SpelFunction(cat=util, desc="Commits all staged changes in the local git repository with the given message.",
            returns="The commit SHA of the new commit")
    public String commit(
            @SpelFunctionParam(name="sourceDir", desc="directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name="message", desc="commit message") String message) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            var commitResult = git.commit()
                .setMessage(message)
                .setAuthor("fcli", "fcli@fortify.com")
                .call();
            var sha = commitResult.getId().getName();
            log.debug("commit: Committed message={}, sha={}", message, sha);
            log.info("Committed changes: {}", sha);
            return sha;
        } catch (GitAPIException e) {
            throw new FcliSimpleException("Failed to commit: " + e.getMessage());
        }
    }

    @SpelFunction(cat=util, desc="Pushes the current branch to the remote repository. Uses token-based authentication from CI environment variables (GITHUB_TOKEN, CI_JOB_TOKEN, SYSTEM_ACCESSTOKEN, BITBUCKET_TOKEN) if available.",
            returns="The name of the remote ref that was pushed")
    public String push(
            @SpelFunctionParam(name="sourceDir", desc="directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            var credentialsProvider = detectCredentialsProvider();
            if (credentialsProvider == null) {
                var remoteUrl = git.getRepository().getConfig().getString("remote", "origin", "url");
                if (remoteUrl != null && remoteUrl.startsWith("https")) {
                    throw new FcliSimpleException("No credentials available for push to " + remoteUrl
                        + ". Set one of: GITHUB_TOKEN, GH_TOKEN, CI_JOB_TOKEN, SYSTEM_ACCESSTOKEN, BITBUCKET_TOKEN");
                }
            }
            var pushCommand = git.push();
            if (credentialsProvider != null) {
                pushCommand.setCredentialsProvider(credentialsProvider);
            }
            var results = pushCommand.call();
            log.debug("push: Successfully pushed branch={} to remote", results);
            var ref = git.getRepository().getFullBranch();
            log.info("Pushed branch to remote: {}", ref);
            return ref;
        } catch (GitAPIException | IOException e) {
            throw new FcliSimpleException("Failed to push: " + e.getMessage());
        }
    }

    @SpelFunction(cat=util, desc="Detects the default branch of the remote repository. Checks CI environment variables (CI_DEFAULT_BRANCH for GitLab, looks up via GitHub API env), then falls back to reading refs/remotes/origin/HEAD from the local git config. Returns null if detection fails.",
            returns="The default branch name (e.g. 'main', 'master', 'develop') or null if not detectable")
    public String defaultBranch(
            @SpelFunctionParam(name="sourceDir", desc="directory inside a git working tree") String sourceDir) {
        // GitLab CI provides CI_DEFAULT_BRANCH
        var defaultBranch = EnvHelper.env("CI_DEFAULT_BRANCH");
        if (StringUtils.isNotBlank(defaultBranch)) {
            log.debug("defaultBranch: Detected from CI_DEFAULT_BRANCH={}", defaultBranch);
            return defaultBranch;
        }
        // Try reading from local git remote HEAD (set by git clone)
        try (var git = openGit(sourceDir)) {
            if (git == null) { return null; }
            var repo = git.getRepository();
            var remoteHead = repo.resolve("refs/remotes/origin/HEAD");
            if (remoteHead != null) {
                var ref = repo.exactRef("refs/remotes/origin/HEAD");
                if (ref != null && ref.getTarget() != null) {
                    var target = ref.getTarget().getName();
                    // target is like refs/remotes/origin/main
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

    private Git openGit(String sourceDir) {
        if (StringUtils.isBlank(sourceDir)) { return null; }
        var dir = Path.of(sourceDir).toAbsolutePath().normalize().toFile();
        if (!dir.exists()) { return null; }
        var builder = new FileRepositoryBuilder().findGitDir(dir);
        if (builder.getGitDir() == null) { return null; }
        try {
            var repo = builder.build();
            return new Git(repo);
        } catch (Exception e) {
            return null;
        }
    }

    private CredentialsProvider detectCredentialsProvider() {
        // GitHub Actions / GitHub CLI
        var token = EnvHelper.env("GITHUB_TOKEN");
        if (StringUtils.isBlank(token)) { token = EnvHelper.env("GH_TOKEN"); }
        if (StringUtils.isNotBlank(token)) {
            return new UsernamePasswordCredentialsProvider("x-access-token", token);
        }
        // GitLab CI
        token = EnvHelper.env("CI_JOB_TOKEN");
        if (StringUtils.isNotBlank(token)) {
            return new UsernamePasswordCredentialsProvider("gitlab-ci-token", token);
        }
        // Azure DevOps
        token = EnvHelper.env("SYSTEM_ACCESSTOKEN");
        if (StringUtils.isNotBlank(token)) {
            return new UsernamePasswordCredentialsProvider("", token);
        }
        // Bitbucket Pipelines
        token = EnvHelper.env("BITBUCKET_TOKEN");
        if (StringUtils.isNotBlank(token)) {
            return new UsernamePasswordCredentialsProvider("x-token-auth", token);
        }
        return null;
    }

    private static String selectRemote(Repository repo) {
        try {
            var remotes = repo.getRemoteNames();
            if (remotes == null || remotes.isEmpty()) { return null; }
            if (remotes.contains("origin")) { return "origin"; }
            return remotes.iterator().next();
        } catch (Exception e) { return null; }
    }

    private static String[] deriveRepoNames(String fallbackShort, String remoteUrl) {
        if (StringUtils.isBlank(remoteUrl)) { return new String[]{fallbackShort, null}; }
        try {
            var cleaned = remoteUrl.trim();
            if (cleaned.endsWith(".git")) { cleaned = cleaned.substring(0, cleaned.length() - 4); }
            String pathPart;
            if (cleaned.startsWith("git@")) {
                int idx = cleaned.indexOf(":");
                pathPart = idx >= 0 ? cleaned.substring(idx + 1) : cleaned;
            } else {
                var uri = java.net.URI.create(cleaned);
                pathPart = uri.getPath();
                if (pathPart.startsWith("/")) { pathPart = pathPart.substring(1); }
            }
            var parts = pathPart.split("/");
            if (parts.length >= 2) {
                var shortName = parts[parts.length - 1];
                return new String[]{shortName, pathPart};
            }
            return new String[]{parts[parts.length - 1], null};
        } catch (Exception e) {
            return new String[]{fallbackShort, null};
        }
    }
}
