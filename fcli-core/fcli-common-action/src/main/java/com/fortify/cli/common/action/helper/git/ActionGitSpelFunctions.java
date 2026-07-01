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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.CiBranch;
import com.fortify.cli.common.ci.CiCommit;
import com.fortify.cli.common.ci.CiCommitId;
import com.fortify.cli.common.ci.CiCommitMessage;
import com.fortify.cli.common.ci.CiGitCredentials;
import com.fortify.cli.common.ci.CiGitCredentialsHelper;
import com.fortify.cli.common.ci.CiPerson;
import com.fortify.cli.common.ci.CiRepository;
import com.fortify.cli.common.ci.CiRepositoryName;
import com.fortify.cli.common.ci.LocalRepoInfo;
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
                headId: { full, short },
                mergeId: { full, short },
                message: { short, full },
                author: { name, email, when },
                committer: { name, email, when }
              }
            }
            """, returns = "Git repository information or null if not a git work dir", returnType = LocalRepoInfo.class)
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
            } catch (Exception e) {
                /* ignore */ }

            CiCommit commit = null;
            var headId = repo.resolve("HEAD");
            if (headId != null) {
                try (var walk = new RevWalk(repo)) {
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

    @SpelFunction(cat = util, desc = """
            Captures a snapshot of the paths that currently have uncommitted changes in the working tree
            (modified, added, removed, missing, changed, conflicting, or untracked). This snapshot can be
            passed to #git.commitChangesSince to commit only the changes introduced afterwards, leaving
            pre-existing changes (e.g. build output) untouched. Structure: { paths: [ "relative/path", ... ] }
            """, returns = "Snapshot of currently dirty paths, or null if the directory is not a git working tree")
    public ObjectNode status(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                return null;
            }
            var root = JsonHelper.getObjectMapper().createObjectNode();
            var paths = root.putArray("paths");
            collectDirtyPaths(git.status().call()).forEach(paths::add);
            return root;
        } catch (GitAPIException e) {
            throw new FcliSimpleException("Failed to determine git status: " + e.getMessage());
        }
    }

    @SpelFunction(cat = util, desc = """
            Creates and checks out a new branch, stages only the changes introduced since the given snapshot (as
            returned by #git.status), and commits them with the given author and message. Paths that were already
            dirty in the snapshot (e.g. build artifacts) are left uncommitted. Returns null without creating a
            branch or commit if there are no new changes to commit.
            """, returns = "The commit SHA, or null if there were no new changes to commit")
    public String commitChangesSince(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name = "snapshot", desc = "dirty-paths snapshot from #git.status") JsonNode snapshot,
            @SpelFunctionParam(name = "branchName", desc = "full branch name to create and checkout") String branchName,
            @SpelFunctionParam(name = "message", desc = "commit message") String message,
            @SpelFunctionParam(name = "name", desc = "commit author name") String name,
            @SpelFunctionParam(name = "email", desc = "commit author email") String email) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            var newPaths = collectDirtyPaths(git.status().call());
            newPaths.removeAll(snapshotPaths(snapshot));
            if (newPaths.isEmpty()) {
                return null;
            }
            createAndCheckoutBranch(git, branchName);
            stagePaths(git, newPaths);
            var commit = git.commit()
                    .setMessage(message)
                    .setAuthor(name, email)
                    .setCommitter(name, email)
                    .call();
            return commit.getId().getName();
        } catch (GitAPIException | IOException e) {
            throw new FcliSimpleException("Failed to commit changes: " + e.getMessage());
        }
    }

    @SpelFunction(cat = util, desc = """
            Pushes the given branch to the remote repository. For HTTPS remotes, authentication uses the
            GIT_PUSH_TOKEN environment variable if set, falling back to the token of the active CI system; if
            neither is available or authentication fails, the push is retried using the local git configuration
            (e.g. an http.extraHeader injected by the CI checkout step). For SSH remotes, the configured SSH keys
            are used. Returns the pushed ref.
            """, returns = "The name of the remote ref that was pushed")
    public String push(
            @SpelFunctionParam(name = "sourceDir", desc = "directory inside a git working tree") String sourceDir,
            @SpelFunctionParam(name = "branchName", desc = "name of the branch to push") String branchName) {
        try (var git = openGit(sourceDir)) {
            if (git == null) {
                throw new FcliSimpleException("Not a git repository: " + sourceDir);
            }
            var repo = git.getRepository();
            var remote = StringUtils.defaultString(selectRemote(repo), "origin");
            ensureOnBranch(git, branchName);
            var remoteUrl = repo.getConfig().getString("remote", remote, "url");
            var refSpec = new RefSpec("refs/heads/" + branchName + ":refs/heads/" + branchName);
            var credentialsProvider = resolveCredentialsProvider(remoteUrl);
            try {
                push(git, remote, refSpec, credentialsProvider);
            } catch (Exception e) {
                if (credentialsProvider == null || !isLikelyAuthFailure(e)) {
                    throw e;
                }
                log.debug("Push using resolved credentials failed ({}); retrying with local git configuration",
                        rootMessage(e));
                push(git, remote, refSpec, null);
            }
            return "refs/heads/" + branchName;
        } catch (FcliSimpleException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to push branch " + branchName + ": " + rootMessage(e), e);
        }
    }

    private void push(Git git, String remote, RefSpec refSpec, CredentialsProvider credentialsProvider)
            throws GitAPIException {
        var pushCmd = git.push().setRemote(remote).setRefSpecs(refSpec).setTimeout(300);
        if (credentialsProvider != null) {
            pushCmd.setCredentialsProvider(credentialsProvider);
        }
        verifyPushResults(pushCmd.call(), remote);
    }

    @SpelFunction(cat = util, desc = "Detects the default branch of the remote repository. Checks CI environment variables (CI_DEFAULT_BRANCH), then falls back to reading refs/remotes/<remote>/HEAD from the local git config. Returns null if detection fails.", returns = "The default branch name (e.g. 'main', 'master', 'develop') or null if not detectable")
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
            var remoteDefaultBranch = detectDefaultBranchFromRemoteHeads(repo);
            if (StringUtils.isNotBlank(remoteDefaultBranch)) {
                return remoteDefaultBranch;
            }
        } catch (Exception e) {
            log.debug("Error detecting default branch", e);
        }
        return null;
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

    private void ensureOnBranch(Git git, String branchName) throws GitAPIException, IOException {
        if (branchName.equals(git.getRepository().getBranch())) {
            return;
        }
        try {
            git.checkout().setName(branchName).call();
        } catch (RefNotFoundException e) {
            git.checkout().setCreateBranch(true).setName(branchName).setStartPoint("HEAD").call();
        }
    }

    private void createAndCheckoutBranch(Git git, String branchName) throws GitAPIException, IOException {
        git.checkout().setCreateBranch(true).setName(branchName).call();
        if (!branchName.equals(git.getRepository().getBranch())) {
            throw new FcliSimpleException("Failed to checkout branch " + branchName);
        }
    }

    private void stagePaths(Git git, Set<String> paths) throws GitAPIException {
        var workTree = git.getRepository().getWorkTree();
        var addNew = git.add();
        var addDeleted = git.add().setUpdate(true);
        var hasNew = false;
        var hasDeleted = false;
        for (var path : paths) {
            if (new File(workTree, path).exists()) {
                addNew.addFilepattern(path);
                hasNew = true;
            } else {
                addDeleted.addFilepattern(path);
                hasDeleted = true;
            }
        }
        if (hasNew) {
            addNew.call();
        }
        if (hasDeleted) {
            addDeleted.call();
        }
    }

    private static Set<String> collectDirtyPaths(Status status) {
        var paths = new TreeSet<String>();
        paths.addAll(status.getModified());
        paths.addAll(status.getChanged());
        paths.addAll(status.getAdded());
        paths.addAll(status.getRemoved());
        paths.addAll(status.getMissing());
        paths.addAll(status.getUntracked());
        paths.addAll(status.getConflicting());
        return paths;
    }

    private static Set<String> snapshotPaths(JsonNode snapshot) {
        var paths = new TreeSet<String>();
        if (snapshot != null && snapshot.get("paths") instanceof ArrayNode arr) {
            arr.forEach(node -> paths.add(node.asText()));
        }
        return paths;
    }

    private static CredentialsProvider resolveCredentialsProvider(String remoteUrl) {
        CiGitCredentials credentials = CiGitCredentialsHelper.resolvePushCredentials(remoteUrl);
        if (credentials == null) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider(credentials.username(), credentials.token());
    }

    private static void verifyPushResults(Iterable<PushResult> results, String remote) {
        var updated = false;
        for (var result : results) {
            for (var update : result.getRemoteUpdates()) {
                switch (update.getStatus()) {
                    case OK:
                    case UP_TO_DATE:
                        updated = true;
                        break;
                    default:
                        throw new FcliSimpleException("Push to " + remote + " rejected: status=" + update.getStatus()
                                + (update.getMessage() != null ? " (" + update.getMessage() + ")" : ""));
                }
            }
        }
        if (!updated) {
            throw new FcliSimpleException("Push to " + remote
                    + " did not update any refs (likely an authentication or permission issue)");
        }
    }

    private static boolean isLikelyAuthFailure(Throwable e) {
        for (var cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof TransportException) {
                return true;
            }
            var message = cause.getMessage();
            if (message != null) {
                var lower = message.toLowerCase();
                if (lower.contains("401") || lower.contains("403") || lower.contains("auth")
                        || lower.contains("not authorized") || lower.contains("forbidden")
                        || lower.contains("permission")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String detectDefaultBranchFromRemoteHeads(Repository repo) {
        var remoteNames = repo.getRemoteNames();
        if (remoteNames == null || remoteNames.isEmpty()) {
            return null;
        }
        try {
            for (var remote : remoteNames) {
                var refName = "refs/remotes/" + remote + "/HEAD";
                var ref = repo.exactRef(refName);
                if (ref != null && ref.getTarget() != null) {
                    var target = ref.getTarget().getName();
                    var prefix = "refs/remotes/" + remote + "/";
                    if (target.startsWith(prefix)) {
                        return target.substring(prefix.length());
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Failed to resolve default branch from remote HEAD", e);
        }
        return null;
    }

    private static String rootMessage(Throwable e) {
        var root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
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
                var uri = URI.create(cleaned);
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

}
