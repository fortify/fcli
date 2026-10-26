# {{var:repo-title}} - Usage Instructions

Fortify CLI usage instructions and manual pages are available online at {{var:gh-pages-url}}. 
Offline versions of the documentation in HTML and man-page formats can be downloaded from the [Releases]({{var:repo-url}}/releases) page.

## Docker Images

Docker images for fcli are built and published in the separate [fortify/fcli-docker](https://github.com/fortify/fcli-docker) repository.

Published images are available at [Docker Hub: fortifydocker/fcli](https://hub.docker.com/r/fortifydocker/fcli).

For more information about available Docker images and usage instructions, see the [fcli-docker repository](https://github.com/fortify/fcli-docker).

## Aviator Connectivity

Ordinary Aviator admin commands and credential validation calls do not wait for a failed connection to recover.
A confirmed connection failure produces an actionable message.
An unavailable response can indicate a connection problem or a temporary server problem.
A deadline can also indicate a busy server.
The request deadline remains 30 seconds.

Audit, streaming, correlation, and connection diagnostics keep their existing wait behavior.
Failed user-login or admin-config validation preserves the existing session or configuration.
This protection applies to validation before saving, not failures after saving.
