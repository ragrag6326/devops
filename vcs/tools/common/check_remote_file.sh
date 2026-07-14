#!/bin/bash
# =============================================================
#  check_remote_file.sh  [prod|dev]  <type: file|dir>  <path>
#  在遠端機器上確認檔案 / 目錄是否存在，輸出 exists 或 not_found
#
#  注意：不可用 TYPE 作變數名，因為 sshToolUtil.sh 內有 TYPE=$1
#        會把本腳本的 TYPE 覆蓋掉
# =============================================================
ENV=$1
CHECK_TYPE=$2   # file | dir  （刻意避開 TYPE，防止被 sshToolUtil.sh 覆蓋）
CHECK_PATH=$3
TOOLS_BASE="/opt/vcs/tools"

source "${TOOLS_BASE}/common/ssh_env.sh"
valid_ssh_env "$ENV" || { echo "error: invalid env $ENV (可用: $(list_ssh_envs))"; exit 1; }
[[ -z "$CHECK_PATH" ]]                     && { echo "error: path required"; exit 1; }

source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$ENV"

if [[ "$CHECK_TYPE" == "dir" ]]; then
    ssh_function "[ -d '${CHECK_PATH}' ] && echo 'exists' || echo 'not_found'"
else
    ssh_function "[ -f '${CHECK_PATH}' ] && echo 'exists' || echo 'not_found'"
fi