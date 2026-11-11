---
name: fcli-review
description: 'Review an fcli local change (self-review before raising a PR) or a pull request (peer review), with test execution and draft comments, against one shared checklist'
disable-model-invocation: true
---
# Review fcli Change

One review standard for everyone. The same checklist (Step 5) applies whether you review your own change before raising a PR or someone else's PR. Only how the code is obtained and how findings are delivered differ. Review as a lead engineer: be specific, constructive, no nitpicks.

## Inputs (ask for anything missing before starting)
- `mode`: `self` or `peer`. If not given, **always ask the user (use the ask_user tool) before doing anything else**; never infer it. Offer `self` (review my current branch or my own PR) and `peer` (review someone else's PR), and ask for the PR number or URL when one applies. If the user gives a PR number, you may suggest a mode by comparing the PR author with `gh api user --jq .login`, but still confirm it.
- `self`: nothing else is needed (reviews the current branch against its base), or a PR number.
- `peer`: PR URL or number and, for fork branches, `owner:branch`. If helpful and available, surface candidate open PRs first.

## Ground rules
1. **Never assume. Ask.** If the requirement, intent, expected behavior or target branch is unclear, ask the user, or raise it as a **Question** to the author.
2. **Verify before claiming.** Confirm every finding by reading the code, searching usages, running tests or running the code involved. If it cannot be confirmed, raise it as a Question, not a defect.
3. **Check how failures behave.** Before calling something silent, risky or broken, confirm whether it fails fast, is logged or is handled elsewhere.
4. **Review only what the change touches.** Report pre-existing issues separately and do not block on them.
5. **Always use the latest code.** Fetch fresh before every review and re-review, and confirm the commit SHA.
6. **Be kind and specific.** Explain why, suggest the smallest safe fix, and phrase uncertain points as requests to confirm.
7. **Post only comments that add value.** Each pending comment must be a specific, verified review comment or question on a line of code, written as a fellow peer would. Do not post praise, thanks, a verdict, a summary or an "approve with changes" note. Drop nits, duplicates of existing threads, and points you cannot verify or that change nothing for the author.
8. **Verify delegated findings.** Treat the output of any sub-agent or tool pass (security, performance) as unverified. Re-read each cited location yourself before it becomes a finding, and say which points you could not confirm.
9. **Follow the review conversation protocol** in `.github/instructions/review-conversation.instructions.md`, so reviewer and author never argue in a thread.

## Step 1: Detect context
- Determine the target branch and base. Default base is `dev/v3.x`; release work targets its feature branch (for example `feat/v3.x/<area>/<release>`).
- `peer`: check `gh auth status`. If not logged in, ask the user to log in themselves and stop.
- `peer`: never touch the user's other branches or uncommitted work. Check `git status` first; if the working tree is not clean, use `git worktree add` for the review (or stop and ask).

## Step 2: Get the code
- `self`: review the current branch in place. Use `git merge-base <base> HEAD`, then list commits and the diff stat. Include uncommitted changes. Do not switch branches.
- `peer`: always check out the code under review.
  - Fork branch: `git fetch https://github.com/<owner>/fcli.git <branch>:review/<owner>-<topic>`
  - PR ref: `pull/<N>/head`
  - Check out the review branch and confirm with `git branch --show-current`.
  - Verify local HEAD equals the PR head SHA (`gh pr view <N> --json headRefOid`) so line numbers match the UI.
  - Find the base with `git merge-base` against the target branch (fetch `upstream` if needed). List commits, authors and the diff stat.
- Always tell the user which branch is checked out.

## Step 3: Read the fcli standards first (single source of truth)
Read, then review explicitly against them. Do not restate their rules; cite them.
- `.github/copilot-instructions.md` (module layout, key rules)
- `.github/instructions/style.instructions.md` (all changes) and `CODE_STYLE.md`
- `.github/instructions/java.instructions.md` (Java changes)
- `.github/instructions/action-yaml.instructions.md` (action YAML changes)
- Skills when relevant: `fcli-command-patterns` (new or changed commands), `fcli-utilities` (check for existing helpers before accepting new ones), `fcli-action-yaml-reference`

## Step 4: Understand and verify before judging
- Read the PR description, linked issues and commit messages. For PRs, verify any literal fenced changelog block matches the actual changes.
- `peer` (and `self` on an existing PR): read all existing review comments, threads and author replies before judging, on the first pass as well as on re-reviews. GitHub: `gh api repos/<owner>/<repo>/pulls/<N>/comments --paginate` and `.../pulls/<N>/reviews`, plus `gh pr view <N> --comments`.
  - For each thread record: reviewer, file:line, the concern, the author's reply, and the commit made after the comment.
  - Check the reply is really in the head (read the code and the commits after the comment); do not trust the reply text.
  - Classify each thread as addressed, partly addressed, declined with a reason, or open. Only reopen a declined or partial one with new evidence.
  - Do not repeat a point already raised. A new finding related to a thread is a fresh comment that references it. Reply on an existing thread only when the user asks, since replies post immediately and cannot be part of a pending review.
- Write down, in at most two sentences each, the **business use case** (requirement, who benefits) and the **behavior change** (what is added, removed or changed for users, scripts and API clients). Check traceability: every change maps to the requirement (flag scope creep), every acceptance criterion is implemented and tested, and the code solves the real problem, not only the literal wording. If the requirement is missing or unclear, ask; do not infer it.
- Read the full final code of every changed production file, not only the diff.
- Grep callers and remaining usages of anything added or changed (old overloads, defaults, duplicated helpers).
- Run the affected tests with Gradle, for example `./gradlew :fcli-core:<module>:test`. Also run each new test class in isolation to catch order dependence, such as a missing `FcliExecutionContextHolder.pushNew()`/`pop()`. Run `./gradlew build` for broad changes. Run `:fcli-other:fcli-functional-test:ftest` only when CLI behaviour changed and a target environment is available.
- `self`: skip tests you already ran on this exact change in the current session, and say so.
- Record the exact commands, pass/fail counts and root causes of failures. Separate failures caused by the change from existing failures (check against the base branch).

## Step 5: Review checklist (fcli specific)

**Architecture and design**
- Commands only parse and orchestrate; logic lives in helpers; container commands have zero business logic.
- Reuses existing abstractions, mixins, `OutputHelperMixins` and `fcli-common-*` utilities instead of new ones. Template-method hooks are as narrow as possible.
- No API or overload sprawl, no long argument lists (use a builder or data object), no type or package mismatch, no dead code or unused APIs.
- Safe defaults: risky or slow behaviour (for example wait-for-ready) is opt-in; implicit contracts are documented.
- Module boundaries respected: `fcli-common-thirdparty` is not modified; MCP SDK only in `fcli-ai-assist`; `picocli-codegen` only in `fcli-app`.
- Behaviour is generic and dynamic: no hard-coded lists of known or unexpected types (for example SSC scan types or engines) where a positive "expected data present" check works.

**Java and style**
- Java 17 idioms; Lombok conventions (`@Slf4j`, no `@Value`/`@With`/`@SuperBuilder`, `@EqualsAndHashCode(callSuper = true)` with `@Data` inheritance; Jackson classes use `@NoArgsConstructor` + `@Data`, not `@Builder`).
- No wildcard or unused imports, no FQCNs, imports ordered per `CODE_STYLE.md`; line length <= 140; `.editorconfig` respected.
- `Optional` used sparingly; collections returned empty, not null; `Objects.requireNonNull` for mandatory params.
- Comments explain why, not what; Javadoc for public types and methods.

**Exceptions and messages**
- Only `Fcli*Exception`, module-specific exceptions or picocli exceptions; no raw `IllegalArgumentException`, `IllegalStateException` or `RuntimeException` for user-facing or command-flow errors.
- Checked exceptions are wrapped in `FcliTechnicalException`; runtime exceptions are re-thrown as is.
- Messages are actionable, sentence case, no trailing period; user-facing strings are externalized to `*Messages.properties`.
- Any new command has `fcli.<path>.usage.header` (validated by `FortifyCLITest`), is registered in its parent, and has no `descriptionKey`. `@DisableTest` is used only for a genuine conflict.

**HTTP, security and privacy**
- Unirest: `headerReplace(name, value)` with `HttpHeader.*`; never `accept()`, `contentType()` or `header()`.
- No credentials, tokens or proxy details in messages, logs or exception cause chains (`@MaskValue`, `LogMaskHelper`). Input is validated. Paths are resolved against an expected root, with zip-slip protection via `FileUtils`. No `enableDefaultTyping()`. New dependencies are justified.
- No keys, secrets or large binaries in the diff or in commit history, even if later removed.

**Action YAML** (if changed)
- SpEL scope rules, TemplateExpression properties and error handling follow `action-yaml.instructions.md`.

**Tests**
- Unit tests cover positive, negative and fallback paths. They are deterministic: no fixed ports, sleeps or timing assumptions.
- Execution context is set up. Test-only code does not leak into production.
- Functional tests (Groovy/Spock) are added for user-visible CLI behaviour.

**Scope and hygiene**
- Only intended changes: no unrelated formatting, message or import churn, and no commits that belong to another PR.
- Commit history is clean: squash fix-ups and reversals, and keep commit messages in conventional style (`feat:`, `fix:`, `chore:`).
- User-visible behaviour changes have doc or changelog updates (do not hand-edit `CHANGELOG.md` unless the repo says so).

**Positives** (for the user only, never posted): note what is done well.

**Breaking changes and guardrails**
- Assume existing behavior and contracts (command names, options, output columns and JSON, action YAML, `.proto` copies) are preserved. Recommend a breaking change only when current behavior is clearly wrong or unsafe and there is no reasonable non-breaking fix; then state the exact impact.
- Do not recommend churn-only refactors with no practical payoff.

## Step 6: Report findings (show the user first)
- Severity: Critical (security, data loss, broken core flow; must fix), High (likely bug or contract break; fix before merge), Medium (real risk or maintainability problem; fix or justify), Low (nit or optimization; does not block), **Question** (unconfirmed or needs the author's input; never presented as a defect).
- Review scope and verdict: Approve, Approve with changes, or Request changes (`self`: Ready to raise, or Fix first). The verdict is for the user only and is never posted. State the review scope explicitly when you intentionally exclude modules or files.
- Findings grouped by severity, each with:
  - `file:line` (new-file line numbers, on lines present in the diff)
  - the problem and why it matters in this specific change
  - a concrete fix or snippet
- Then: **Existing threads** (per thread, its status (addressed, partly, declined, open) with evidence from the head commit, and whether a reply is warranted), Questions to the author (what you checked and what you need confirmed), pre-existing issues (not blocking), and what was verified versus not verified.
- Avoid questions you can answer yourself (for example whether a copy matches another repository, or whether callers exist): check first and raise only what the author alone can answer.
- Before asking for approval, mark each finding and question "post" or "drop", and say why for each drop.
- Add test results and the exact commands used.
- `peer`: comments are written to the author. Wait for the user's explicit OK or edits before posting anything.
- `self`: do not change code unless the user asks. When asked, fix, re-run the affected tests and re-report.

## Step 7: Deliver
- `self`: the report in Step 6 is the deliverable. Remind the user what is left before raising the PR.
- `peer`: post as a **pending/draft review** only, with inline comments on the PR head SHA:
  - `gh api -X POST repos/<owner>/<repo>/pulls/<N>/reviews` with `commit_id`, `body` and `comments[]`, and **no `event` field**. Pipe the JSON through stdin (`--input -`); don't write temp files.
  - For lines outside the diff, anchor to the nearest changed line and state the real location in the text. Leave the review body empty: no summary, verdict, positives or history-hygiene remarks. Give those to the user in chat only.
  - **Never submit, publish, approve, request changes, merge or resolve.** The user does the final manual review in the UI.
  - If drafts are unsupported or posting fails, output ready-to-paste comments instead.
  - Finish with the review URL, the comment count and confirmation that the state is PENDING/draft. Remind the user which review branch is checked out and how to return to their previous branch.

## Step 8: Re-review after the author updates the PR
- Fetch and verify the head SHA again (Step 2) before reading anything.
- Re-read every existing thread, including resolved ones, with the author's replies and the commits made after each comment (Step 4). Check in the head code that each reply is really implemented, and update each thread's status.
- Review the new commits compared with the previously reviewed SHA. Also look at the rest of the change for anything the earlier pass missed, including problems the new commits introduce next to the fixes.
- Withdraw or correct any earlier comment that turns out to be wrong, plainly and politely.
- Do not repeat comments that were already addressed, and do not post replies that only confirm a fix or say thanks. Post a reply only when it adds a new, verified point.

## Cleanup
- Never commit or push from a review checkout. When finished, offer to remove the review branch or worktree and let the user decide.
