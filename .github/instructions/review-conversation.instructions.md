---
applyTo: ".github/**"
description: "Shared rules for pull request review threads between a reviewer and an author, so AI-assisted developers never argue in a thread"
---

# Review conversation protocol

Reviewers (`/fcli-review peer`) and authors (`/fcli-address-review-comments`) work from this same protocol, so two people using AI never end up arguing in a thread. Both skills follow it and do not restate it. The review checklist and severity levels live in `.github/skills/fcli-review/SKILL.md`.

1. **One basis for decisions, in this order:** the requirement and acceptance criteria, then the repository instruction files (`.github/instructions`, `CODE_STYLE.md`), then the review checklist, then personal preference. A comment or reply that rests only on preference is at most a Low or a Question, and may be declined without further discussion.
2. **Argue with evidence only:** file and line, test output, a documented rule, or observed behavior. Never argue from tone, assumption or "best practice" with no source.
3. **Shared thread states:** addressed, partly addressed, declined with a reason, or open. An author's outcome maps to these: fixed is addressed, fixed another way is partly addressed, not changed with evidence is declined, a question back to the reviewer is open.
4. **Questions are answered, not defended.** A Question is answered with evidence. It is never treated as a defect, and the answer is never treated as a refusal.
5. **Concede when the other side is right.** A reviewer withdraws a point that the author's evidence disproves, and an author fixes a point that the reviewer's evidence proves. Withdrawing is done in chat to the developer. No thanks or "good point" replies are posted.
6. **Severity is respected.** A Low or nit does not block. An author may decline it with a short reason, and the reviewer does not reopen it.
7. **A declined thread is reopened only with new evidence,** and at most once. After one reviewer comment, one author reply and one follow-up with new evidence, both AIs stop posting on that thread. They tell their developers it needs a human conversation.
8. **Humans decide and resolve.** Neither AI submits, resolves, approves or merges. The reviewer resolves their own threads, and each developer submits their own drafts after reading them.
9. **Scope stays fixed.** Pre-existing issues are reported separately and do not block the PR. Out-of-scope ideas are tracked outside the PR, and no work item or issue is created without approval.
