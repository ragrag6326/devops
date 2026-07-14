#!/bin/bash
# =============================================================
#  write_docker_compose.sh  <sshEnv>  <composePath>  <localTmpFile>
#
#  sshEnv     : prod | dev (決定 SSH 連到哪台機器)
#  composePath: 遠端完整路徑（由後端決定）
#  localTmpFile: 本機暫存檔路徑
# =============================================================
SSH_ENV=$1
COMPOSE_PATH=$2
TMP_FILE=$3
TOOLS_BASE="/opt/vcs/tools"

source "${TOOLS_BASE}/common/ssh_env.sh"
valid_ssh_env "$SSH_ENV" || { echo "❌ 未知 sshEnv '${SSH_ENV}'，可用: $(list_ssh_envs)"; exit 1; }
[[ -z "$COMPOSE_PATH" ]]  && { echo "❌ composePath required"; exit 1; }
[[ ! -f "$TMP_FILE" ]]    && { echo "❌ 暫存檔不存在: $TMP_FILE"; exit 1; }

source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$SSH_ENV"

# 確保遠端目錄存在
REMOTE_DIR=$(dirname "$COMPOSE_PATH")
ssh_function "mkdir -p '${REMOTE_DIR}'"

cat "$TMP_FILE" | ssh_function "cat > '${COMPOSE_PATH}'"
if [[ $? -eq 0 ]]; then
    echo "✅ docker-compose.yml 已寫入 ${SSH_ENV}: ${COMPOSE_PATH}"
else
    echo "❌ 寫入失敗"
    exit 1
fi
