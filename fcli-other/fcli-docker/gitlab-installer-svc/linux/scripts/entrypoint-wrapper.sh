#!/bin/sh
set -o errexit
if [ "true" = "${CI_DEBUG_TRACE}" ]; then
  set -x
fi
WORKDIR=${CI_BUILDS_DIR:-/builds}/fortify
mkdir -p "${WORKDIR}"
entrypoint.sh "${WORKDIR}" 2>&1 | tee "${WORKDIR}/fcli-install.log"
