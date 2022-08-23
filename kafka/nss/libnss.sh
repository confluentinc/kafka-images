#!/usr/bin/env bash

. /bin/liblog.sh

########################
# Configure libnss_wrapper so PostgreSQL commands work with a random user.
# Globals:
#   POSTGRESQL_*
# Arguments:
#   None
# Returns:
#   None
#########################
enable_nss_wrapper() {
  export DAEMON_USER="${DAEMON_USER:-appuser}"
  export DAEMON_GROUP="${DAEMON_GROUP:-appuser}"
  export DAEMON_BIN_DIR="${DAEMON_BIN_DIR:-/tmp}"
  export NSS_WRAPPER_LIB="${NSS_WRAPPER_LIB:-./bin/libnss.sh}"
  if ! getent passwd "$(id -u)" &> /dev/null && [ -e "$NSS_WRAPPER_LIB" ]; then
    debug "Configuring libnss_wrapper..."
    export LD_PRELOAD="$NSS_WRAPPER_LIB"
    # shellcheck disable=SC2155
    export NSS_WRAPPER_PASSWD="$(mktemp)"
    # shellcheck disable=SC2155
    export NSS_WRAPPER_GROUP="$(mktemp)"
    echo "$DAEMON_USER:x:$(id -u):$(id -g):appuser $DAEMON_USER:$DAEMON_BIN_DIR:/bin/false" > "$NSS_WRAPPER_PASSWD"
    echo "$DAEMON_GROUP:x:$(id -g):" > "$NSS_WRAPPER_GROUP"
  fi
}