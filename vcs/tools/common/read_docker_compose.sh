#!/bin/bash
# =============================================================
#  read_docker_compose.sh  <sshEnv>  <composePath>
#
#  sshEnv     : prod | dev (決定 SSH 連到哪台機器)
#  composePath: 遠端完整路徑（由後端決定，不在此處硬寫）
#               PROD / form-service: /opt/docker_image/<project>/docker-compose.yml
#               DEV 共用:            /opt/docker_image/docker-compose.yml
# =============================================================
SSH_ENV=$1
COMPOSE_PATH=$2
TOOLS_BASE="/opt/vcs/tools"

[[ "$SSH_ENV" != "prod" && "$SSH_ENV" != "dev" ]] && { echo "❌ sshEnv must be prod or dev"; exit 1; }
[[ -z "$COMPOSE_PATH" ]] && { echo "❌ composePath required"; exit 1; }

source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$SSH_ENV"

ssh_function "[ -f '${COMPOSE_PATH}' ] && cat '${COMPOSE_PATH}' || echo '__NOT_FOUND__'"
