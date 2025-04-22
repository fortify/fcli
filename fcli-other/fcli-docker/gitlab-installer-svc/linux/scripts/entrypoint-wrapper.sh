#!/bin/sh
set -o errexit
if [ "true" = "${CI_DEBUG_SERVICES}" ]; then
  set -x
fi
if [ -z "${FCLI_INSTALL_DIR}" ]; then
  FCLI_INSTALL_DIR=${CI_BUILDS_DIR:-/builds}/fortify
fi
mkdir -p "${FCLI_INSTALL_DIR}"
entrypoint.sh "${FCLI_INSTALL_DIR}" 2>&1 | tee "${FCLI_INSTALL_DIR}/fcli-install.log"
