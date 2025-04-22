#!/bin/sh
set -o errexit
if [ "true" = "${CI_DEBUG_SERVICES}" ]; then
  set -x
fi

INSTALL_DIR=$1

echo "Install fcli: $($FCLI_DIR/fcli -V) to $INSTALL_DIR"
cp "$FCLI_DIR/fcli" "$INSTALL_DIR"

# Allow GitLab service readyness probe to succeed
echo "Listening for GitLab readiness check"
socat tcp-listen:9999,reuseaddr,fork exec:cat,nofork
