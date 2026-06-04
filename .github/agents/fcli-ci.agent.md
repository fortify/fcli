---
description: "Edit fcli CI/CD workflows (GitHub Actions)"
---

# fcli GitHub Actions Agent

You edit GitHub Actions workflow files for the fcli repository.

## Workflow Structure

- Main CI: `.github/workflows/ci.yml` — build, native images, release, Docker trigger
- Functional tests: `.github/workflows/verify-release.yml`
- 3rd-party actions are proxied via `fortify/3rdparty-actions/actions/` (e.g., `fortify/3rdparty-actions/actions/googleapis/release-please-action/v4@main`)

## Build Commands

- `./gradlew clean build dist distThirdPartyReleaseAsset distFtest -Pversion=$VERSION -PautoFormat=false`
- Creates: `build/libs/fcli.jar`, `build/dist/release-assets/*`, `build/dist/fcli-ftest.jar`

## Key Conventions

- Use `fortify/3rdparty-actions/actions/` prefix for external actions
- Tag management via `fortify/.github/.github/actions/update-tag@main`
- Dev releases use `dev_<branch>` tags; prod releases use release-please
- Native images: Linux (static/musl), macOS (dynamic), Windows (dynamic)
- Functional tests triggered via `gh workflow run verify-release.yml`
- Docker builds triggered in separate `fortify/fcli-docker` repository

## Security

- Use `${{ secrets.* }}` for tokens; never hardcode
- Use `env:` blocks to pass secrets and untrusted data to steps (not inline in `run:`)
- `permissions: read-all` at workflow level; elevated permissions only on specific jobs
