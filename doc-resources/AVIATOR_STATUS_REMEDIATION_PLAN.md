# Aviator Status Based on Remediation Availability

## Scope

This plan covers these ValueEdge items:

- [Story 2460262: Set status based on remediation availability](https://almoctane-ams.saas.microfocus.com/ui/entity-navigation?p=206015/2001&entityType=work_item&id=2460262)
- [Story 2459282: Support status values across SSC versions](https://almoctane-ams.saas.microfocus.com/ui/entity-navigation?p=206015/2001&entityType=work_item&id=2459282)
- [Story 2453312: Support both status values in SSC](https://almoctane-ams.saas.microfocus.com/ui/entity-navigation?p=206015/2001&entityType=work_item&id=2453312)
- [Feature 2238118: Differentiate audits with and without auto-remediation](https://almoctane-ams.saas.microfocus.com/ui/entity-navigation?p=206015/2001&entityType=work_item&id=2238118)

The tag name remains `Aviator status`.
The tag GUID remains `FB7B0462-2C2E-46D9-811A-DCC1F3C83051`.

## Required behavior

- Write `PROCESSED_BY_AVIATOR_WITH_REMEDIATION` when the latest SAST response has non-empty `autoremediation.changes`.
- Write `PROCESSED_BY_AVIATOR` when changes are absent, null, or empty.
- Apply the status to existing and new SAST issues and their audit history.
- Re-audits must support transitions in either direction.
- Do not retroactively change issues that receive no new audit response.
- Keep DAST status behavior unchanged.

## Implementation plan

1. Add the new status constant in `fcli-aviator-common`.
2. Add one null-safe status resolver in `AuditProcessor`.
3. Use the same resolved value for SAST issue tags and audit history.
4. Keep skipped issue entries and DAST writes on the existing status.
5. Update `IssueAuditor` to recognize both values as processed.
6. Preserve force re-audit and human-triage safeguards.
7. Add both values to FPR filter-template provisioning.
8. Update existing FPR definitions in both the parsed model and XML.
9. Keep FPR provisioning idempotent and preserve value order.
10. Add both values to the SSC Aviator status definition.
11. On SSC 25.4, create or update the user-managed tag without duplicates.
12. On SSC 26.2 and later, detect system-managed ownership before any write.
13. Update pre-upload validation to report when the new value is unavailable.
14. Update SSC setup guidance, usage documentation, and release notes.

## SSC ownership and compatibility

Check `/internalCustomTags` before checking or changing `/customTags`.
Match the tag GUID and the system-managed tag type.
Never update or replace a system-managed tag definition.

Treat only a confirmed unsupported endpoint response as an older SSC version.
Do not treat authentication, permission, server, or response parsing errors as proof of an older version.
On those errors, report failure and do not create or update a tag.

SSC 26.2+ may expose the Aviator status tag as a system-managed tag.
The fcli prepare command must not modify that tag.
The pre-upload validator checks that the new value is available.
If the value is absent, fcli warns and continues with the upload.
For older SSC versions, prepare adds the value to the user-managed tag.

## Tests

- SAST responses with no remediation object, null changes, empty changes, and non-empty changes.
- Existing and new issue tags and audit history.
- Both re-audit transitions, with previous audit history preserved.
- Normal audit and force re-audit handling for both status values.
- Human-triage safeguards for issues with the new status.
- FPR definitions that are missing, legacy, complete, and processed repeatedly.
- FPR model and XML value IDs remain unique when existing IDs are non-sequential.
- SSC 25.4 tag creation, missing-value updates, and repeated setup.
- SSC 26.2+ system-managed detection, including tags also listed by `/customTags`.
- Assert that SSC system-managed tags receive no create or update requests.
- Warn before upload when either user-managed or system-managed SSC status values are missing.
- Assert that DAST continues to write only `PROCESSED_BY_AVIATOR`.
- Assert that issues without a new response are not backfilled.

## Main risks and decisions

- SSC system-managed definitions may not include the new value. The validator warns and continues.
- A non-empty response patch may not produce a remediation entry in the output FPR.
- FPR model updates and XML updates may diverge.
- Broad error handling may lead fcli to modify a system-managed tag.
- A force re-audit changes an issue's current status, but does not count as backfill.

Status depends on non-empty response `autoremediation.changes`, as the story requires.
The output FPR remediation file does not decide the status.

## Validation

Run the focused Aviator tests first:

```powershell
.\gradlew.bat :fcli-core:fcli-aviator-common:test --tests '*AuditProcessor*' --tests '*IssueAuditorTest' --tests '*AviatorFilterTemplateCustomizerTest' --tests '*DastAuditFPRTest'
.\gradlew.bat :fcli-core:fcli-aviator:test --tests '*AviatorSSCCustomTagHelperTest' --tests '*AviatorSSCPrepareHelperTest' --tests '*AviatorSSCTagValidatorTest'
```

Then run both module test suites and the repository build:

```powershell
.\gradlew.bat :fcli-core:fcli-aviator-common:test :fcli-core:fcli-aviator:test
.\gradlew.bat build
```
