#!/bin/bash
# =============================================================
#  check_remote_file.sh  [prod|dev]  <type: file|dir>  <path>
#  在遠端機器上確認檔案 / 目錄是否存在，輸出 exists 或 not_found
# =============================================================
ENV=$1
TYPE=$2
CHECK_PATH=$3
TOOLS_BASE="/opt/vcs/tools"

[[ "$ENV" != "prod" && "$ENV" != "dev" ]] && { echo "error: invalid env $ENV"; exit 1; }
[[ -z "$CHECK_PATH" ]]                     && { echo "error: path required"; exit 1; }

source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$ENV"

if [[ "$TYPE" == "dir" ]]; then
    ssh_function "[ -d '${CHECK_PATH}' ] && echo 'exists' || echo 'not_found'"
else
    ssh_function "[ -f '${CHECK_PATH}' ] && echo 'exists' || echo 'not_found'"
fi
