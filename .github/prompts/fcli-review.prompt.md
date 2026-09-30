---
description: 'Review an fcli local change or pull request'
---

# Review fcli Change

Review one fcli change end to end.

## Step 0: Identify Target

- If the user did not name a local diff, files, PR number, or PR URL, ask for it first.
- If helpful and available, surface candidate open PRs before reviewing.

## Step 1: Gather Context

- Read the diff, PR description, and nearby code together.
- For PRs, verify any literal fenced changelog block matches the actual changes.
- Note review scope explicitly when you intentionally exclude modules or files.

## Step 1b: Check Instructions

- Before findings, read any repo instruction file that applies to the changed paths.
- Always check `.github/instructions/style.instructions.md` for all fcli changes.
- Check `.github/instructions/java.instructions.md` for Java changes.
- Check `.github/instructions/action-yaml.instructions.md` for action YAML changes.
- Compare the change against those instructions explicitly and call out mismatches.

## Step 2: Review

- Focus on style, architecture, correctness, security, and missing tests.
- Call out long argument lists, type drift, dead code, package mismatches, and input validation gaps.
- Separate code findings from process notes like partial review or excluded modules.

## Step 3: Commenting

- Draft exact comment text first.
- If the user wants comments posted, wait for explicit confirmation before posting anything.
- Use exact file references and concrete fix suggestions.

## Step 4: Output

- Start with review scope if needed.
- Then report findings as Critical, Warning, or Suggestion.
- Keep findings focused and avoid nitpicks.