#!/bin/bash

# ================= 設定區 =================
# 機器對照表：common/ssh_hosts.json（新增機器改 JSON 即可，不用改這支腳本）
# 每筆格式：{"host": "...", "user": "...", "key": "/opt/vcs/tools/key/xxx.pem"}
# 解析邏輯統一在 common/ssh_env.sh（jq → python3 → 內建 prod/dev 三層 fallback）
# 注意：這支腳本會被 source（init.sh），內部不可使用 exit，
#       查表失敗以 SSH_UTIL_READY=false 標記，ssh_function 會擋下並回報原因。
# =========================================

TYPE=$1

# 預設值（對照表缺欄位時的 fallback）
USER="tkb0001662"
HOST="132.145.125.250"
KEY_PATH="/opt/vcs/tools/key/prod.pem"

SSH_UTIL_READY=true
SSH_UTIL_ERROR=""

_SSH_UTIL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

_ssh_util_fail() {
    SSH_UTIL_READY=false
    SSH_UTIL_ERROR="$1"
    echo "[sshToolUtil] $1" >&2
}

# 載入共用的對照表讀取函式（valid_ssh_env / list_ssh_envs / ssh_env_field）
_SSH_ENV_HELPER="${_SSH_UTIL_DIR}/../common/ssh_env.sh"
[ ! -f "${_SSH_ENV_HELPER}" ] && _SSH_ENV_HELPER="/opt/vcs/tools/common/ssh_env.sh"

if [ -f "${_SSH_ENV_HELPER}" ]; then
    source "${_SSH_ENV_HELPER}"
else
    SSH_ENV_FALLBACK_REASON="找不到 ${_SSH_ENV_HELPER}"
fi

if [ -n "${SSH_ENV_FALLBACK_REASON}" ]; then
    # 對照表不可用：退回內建 case（向下相容），只支援 prod/dev
    echo "[sshToolUtil] 警告: 無法讀取 ssh_hosts.json（${SSH_ENV_FALLBACK_REASON}），退回內建 prod/dev" >&2
    case "${TYPE}" in
        prod) KEY_PATH="/opt/vcs/tools/key/prod.pem"; HOST="132.145.125.250" ;;
        dev)  KEY_PATH="/opt/vcs/tools/key/dev.pem";  HOST="131.186.44.40" ;;
        *)    _ssh_util_fail "未知環境 '${TYPE}'（對照表不可用時僅支援 prod | dev）" ;;
    esac
elif [ -z "${TYPE}" ]; then
    _ssh_util_fail "未指定環境參數（\$1），可用環境: $(list_ssh_envs)"
else
    _host=$(ssh_env_field "${TYPE}" "host")
    if [ -z "${_host}" ]; then
        _ssh_util_fail "找不到環境 '${TYPE}'，可用環境: $(list_ssh_envs)"
    else
        HOST="${_host}"
        _user=$(ssh_env_field "${TYPE}" "user")
        [ -n "${_user}" ] && USER="${_user}"
        _key=$(ssh_env_field "${TYPE}" "key")
        if [ -n "${_key}" ]; then
            KEY_PATH="${_key}"
        else
            KEY_PATH="/opt/vcs/tools/key/${TYPE}.pem"
        fi
    fi
fi

# 金鑰存在性檢查（~ 開頭的路徑交給 ssh 自行展開，跳過檢查）
if [ "${SSH_UTIL_READY}" = "true" ] && [ -n "${KEY_PATH}" ]; then
    case "${KEY_PATH}" in
        "~"*) : ;;
        *)
            [ ! -f "${KEY_PATH}" ] && echo "[sshToolUtil] 警告: 金鑰 ${KEY_PATH} 不存在，SSH 可能失敗" >&2
            ;;
    esac
fi

# 連線前的共同防呆：參數不完整就擋下，避免 ssh 把 user@host 當金鑰、把指令當 hostname
_ssh_util_guard() {
    if [ "${SSH_UTIL_READY}" != "true" ]; then
        echo "[sshToolUtil] 中止遠端操作: ${SSH_UTIL_ERROR}" >&2
        return 1
    fi
    if [ -z "${KEY_PATH}" ] || [ -z "${USER}" ] || [ -z "${HOST}" ]; then
        echo "[sshToolUtil] 中止遠端操作: 參數不完整 (USER='${USER}' HOST='${HOST}' KEY_PATH='${KEY_PATH}')" >&2
        return 1
    fi
    return 0
}

ssh_connect_check() {
    _ssh_util_guard || { echo "fail"; return 1; }
    result=$(ssh -C -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i "${KEY_PATH}" "${USER}@${HOST}" "echo success")
    echo ${result}
}

ssh_function() {
    local cmd=$1
    _ssh_util_guard || return 1
    ssh -C -o LogLevel=ERROR -o StrictHostKeyChecking=no -i "${KEY_PATH}" "${USER}@${HOST}" "$cmd"
}

scp_file() {
    local source=$1
    local dist=$2
    _ssh_util_guard || return 1
    scp -o PubkeyAuthentication=yes -o stricthostkeychecking=no -i "${KEY_PATH}" "$source" "$dist"
}
