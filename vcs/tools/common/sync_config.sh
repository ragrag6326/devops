#!/bin/bash
# =============================================================
#  sync_config.sh  [prod|dev]  <scriptName>
#  將本機 /opt/vcs/tools/<scriptName>/config.sh 同步至遠端同路徑
# =============================================================
ENV=$1
SCRIPT_NAME=$2
TOOLS_BASE="/opt/vcs/tools"
CONFIG_PATH="${TOOLS_BASE}/${SCRIPT_NAME}/config.sh"

[[ "$ENV" != "prod" && "$ENV" != "dev" ]] && { echo "❌ env must be prod or dev"; exit 1; }
[[ -z "$SCRIPT_NAME" ]]                    && { echo "❌ scriptName required"; exit 1; }
[[ ! -f "$CONFIG_PATH" ]]                  && { echo "❌ 本機 config.sh 不存在: ${CONFIG_PATH}"; exit 1; }

source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$ENV"

# 確保遠端目錄存在
ssh_function "mkdir -p '${TOOLS_BASE}/${SCRIPT_NAME}'"

# 同步檔案
cat "$CONFIG_PATH" | ssh_function "cat > '${CONFIG_PATH}'"
if [[ $? -eq 0 ]]; then
    echo "✅ config.sh 已同步至 ${ENV}: ${CONFIG_PATH}"
else
    echo "❌ 同步失敗"
    exit 1
fi
