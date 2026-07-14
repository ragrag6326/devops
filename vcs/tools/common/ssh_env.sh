#!/bin/bash
# ============================================================
#  common/ssh_env.sh — 機器對照表(ssh_hosts.json)讀取與環境驗證
#
#  被 sshToolUtil.sh 與 common/* 腳本 source，統一提供：
#    valid_ssh_env <env>          環境是否存在（0/1）
#    list_ssh_envs                可用環境清單（錯誤訊息用）
#    ssh_env_field <env> <field>  取欄位值（host/user/key/label）
#
#  解析器四層 fallback（VCS 後端在 Docker 容器內執行，最小 image 沒有 jq）：
#    1. jq       — PATH ＋ 常見路徑 ＋ /opt/vcs/tools/bin/jq（放靜態 binary 進
#                  已掛載的 tools 目錄即可用，不用改 image）
#    2. python3  — PATH ＋ 常見路徑
#    3. 純 bash  — tr/sed/grep 解析（本 JSON 結構固定：兩層、值皆字串，足夠）
#    4. 內建 prod/dev 白名單（僅剩 json 檔案不存在時）
#  注意：會被 source，內部不可 exit。
# ============================================================

_SSH_ENV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SSH_HOSTS_JSON_FILE="${_SSH_ENV_DIR}/ssh_hosts.json"
[ ! -f "${SSH_HOSTS_JSON_FILE}" ] && SSH_HOSTS_JSON_FILE="/opt/vcs/tools/common/ssh_hosts.json"

# ── 解析器探測 ──────────────────────────────────────────────
_SSH_ENV_JQ="$(command -v jq 2>/dev/null)"
if [ -z "${_SSH_ENV_JQ}" ]; then
    for _c in /opt/vcs/tools/bin/jq /usr/bin/jq /usr/local/bin/jq /snap/bin/jq /bin/jq; do
        if [ -x "${_c}" ]; then
            _SSH_ENV_JQ="${_c}"
            break
        fi
    done
fi
_SSH_ENV_PY=""
if [ -z "${_SSH_ENV_JQ}" ]; then
    _SSH_ENV_PY="$(command -v python3 2>/dev/null)"
    if [ -z "${_SSH_ENV_PY}" ]; then
        for _c in /usr/bin/python3 /usr/local/bin/python3 /bin/python3; do
            if [ -x "${_c}" ]; then
                _SSH_ENV_PY="${_c}"
                break
            fi
        done
    fi
fi

# fallback 原因（僅剩「檔案不存在」需要退回內建白名單；解析永遠有純 bash 層可用）
SSH_ENV_FALLBACK_REASON=""
if [ ! -f "${SSH_HOSTS_JSON_FILE}" ]; then
    SSH_ENV_FALLBACK_REASON="找不到 ${SSH_HOSTS_JSON_FILE}"
fi

# ── 純 bash 解析（tier 3）：擠成單行後用 sed 取值 ─────────────
# 依賴本檔案結構固定：頂層 key → 一層物件、值皆為字串、無巢狀物件
_ssh_env_bash_field() {
    local env=$1 field=$2 flat block
    flat=$(tr -d '\n\r\t' < "${SSH_HOSTS_JSON_FILE}")
    block=$(printf '%s' "${flat}" | sed -n "s/.*\"${env}\"[[:space:]]*:[[:space:]]*{\([^}]*\)}.*/\1/p")
    [ -z "${block}" ] && return 1
    printf '%s' "${block}" | sed -n "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p"
}

_ssh_env_bash_keys() {
    tr -d '\n\r\t' < "${SSH_HOSTS_JSON_FILE}" \
        | grep -oE '"[A-Za-z0-9._-]+"[[:space:]]*:[[:space:]]*\{' \
        | sed 's/"\([^"]*\)".*/\1/' \
        | grep -v '^_' | sort | tr '\n' '|' | sed 's/|$//; s/|/ | /g'
}

# ── 取 .{env}.{field}，讀不到輸出空字串 ───────────────────────
ssh_env_field() {
    local env=$1 field=$2
    [ -z "${env}" ] || [ -z "${field}" ] && return 1
    [ -n "${SSH_ENV_FALLBACK_REASON}" ] && return 1

    if [ -n "${_SSH_ENV_JQ}" ]; then
        "${_SSH_ENV_JQ}" -r --arg e "${env}" --arg f "${field}" \
            '.[$e][$f] // empty' "${SSH_HOSTS_JSON_FILE}" 2>/dev/null
    elif [ -n "${_SSH_ENV_PY}" ]; then
        "${_SSH_ENV_PY}" -c '
import json, sys
try:
    d = json.load(open(sys.argv[3], encoding="utf-8-sig"))
    v = d.get(sys.argv[1])
    print(v.get(sys.argv[2], "") if isinstance(v, dict) else "")
except Exception:
    pass
' "${env}" "${field}" "${SSH_HOSTS_JSON_FILE}" 2>/dev/null
    else
        _ssh_env_bash_field "${env}" "${field}" 2>/dev/null
    fi
}

# ── 環境是否存在（json 不存在時只允許內建 prod/dev）────────────
valid_ssh_env() {
    local env=$1
    [ -z "${env}" ] && return 1
    if [ -n "${SSH_ENV_FALLBACK_REASON}" ]; then
        [ "${env}" = "prod" ] || [ "${env}" = "dev" ]
        return
    fi
    [ -n "$(ssh_env_field "${env}" "host")" ]
}

# ── 可用環境清單（fallback 時附原因）─────────────────────────
list_ssh_envs() {
    if [ -n "${SSH_ENV_FALLBACK_REASON}" ]; then
        echo "prod | dev（fallback：${SSH_ENV_FALLBACK_REASON}）"
        return
    fi
    local keys=""
    if [ -n "${_SSH_ENV_JQ}" ]; then
        keys=$("${_SSH_ENV_JQ}" -r 'keys | map(select(startswith("_") | not)) | join(" | ")' "${SSH_HOSTS_JSON_FILE}" 2>/dev/null)
    elif [ -n "${_SSH_ENV_PY}" ]; then
        keys=$("${_SSH_ENV_PY}" -c '
import json, sys
try:
    d = json.load(open(sys.argv[1], encoding="utf-8-sig"))
    print(" | ".join(sorted(k for k in d if not k.startswith("_"))))
except Exception:
    pass
' "${SSH_HOSTS_JSON_FILE}" 2>/dev/null)
    else
        keys=$(_ssh_env_bash_keys 2>/dev/null)
    fi
    if [ -n "${keys}" ]; then
        echo "${keys}"
    else
        echo "prod | dev（fallback：ssh_hosts.json 解析失敗，請檢查 JSON 格式）"
    fi
}
