#!/bin/bash
# =============================================================
#  init_project.sh  <sshEnv>  <projectName>  <scriptName>  [initType]
#
#  sshEnv    : prod | dev  — 決定 SSH 目標機器
#  initType  : prod | dev  — 決定初始化邏輯（預設同 sshEnv）
#
#  特殊專案（如 form-service）：prod_ssh_env=dev
#    → sshEnv=dev（SSH 打到 dev 機器）, initType=prod（執行 PROD 初始化邏輯）
#
#  initType=prod:
#    - /opt/docker_image/${PROJECT}/                  確保存在
#    - /opt/docker_image/${PROJECT}/.env              建立（若不存在）
#    - /opt/docker_image/${PROJECT}/script/           建立
#    - /opt/docker_image/${PROJECT}/script/deploy.sh  從模板生成
#    - /opt/docker_image/${PROJECT}/script/rollback.sh
#    - /opt/docker_image/${PROJECT}/script/switch_traffic.sh
#
#  initType=dev:
#    - （目前保留，DEV 初始化邏輯視需求補充）
# =============================================================
SSH_ENV=$1
PROJECT=$2      # DB project name（docker 目錄名）
SCRIPT_NAME=$3  # tools/ 目錄名（可能與 PROJECT 不同）
INIT_TYPE=${4:-$SSH_ENV}  # 未傳則預設與 SSH_ENV 相同

TOOLS_BASE="/opt/vcs/tools"
TPL_DIR="${TOOLS_BASE}/common/templates"
DOCKER_IMAGE_BASE="/opt/docker_image"

# Validation
# SSH_ENV = 連線目標機器 → 以 ssh_hosts.json 為準（新增機器不用改本腳本）
source "${TOOLS_BASE}/common/ssh_env.sh"
valid_ssh_env "$SSH_ENV" || { echo "❌ 未知 sshEnv '${SSH_ENV}'，可用環境: $(list_ssh_envs)。Usage: init_project.sh <sshEnv> <project> <scriptName> [initType]"; exit 1; }
# INIT_TYPE = 初始化「邏輯」分支，只有 prod/dev 兩種流程，與機器無關，維持寫死
[[ "$INIT_TYPE" != "prod" && "$INIT_TYPE" != "dev" ]] && { echo "❌ initType 必須是 prod 或 dev（初始化流程種類，非機器名），收到: ${INIT_TYPE}"; exit 1; }
[[ -z "$PROJECT" ]]     && { echo "❌ projectName required"; exit 1; }
[[ -z "$SCRIPT_NAME" ]] && SCRIPT_NAME="$PROJECT"

# SSH 目標由 SSH_ENV 決定
source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$SSH_ENV"

echo "▶ init_project.sh  sshEnv=${SSH_ENV}  initType=${INIT_TYPE}  project=${PROJECT}  scriptName=${SCRIPT_NAME}"

# ──────────────────────────────────────────────────────────────
# PROD 初始化邏輯：建立目錄 + 生成 deploy/rollback/switch scripts
# ──────────────────────────────────────────────────────────────
if [[ "$INIT_TYPE" == "prod" ]]; then
    REMOTE_BASE="${DOCKER_IMAGE_BASE}/${PROJECT}"
    SCRIPT_DIR="${REMOTE_BASE}/script"

    echo "▶ [PROD-init] 建立目錄: ${REMOTE_BASE}"
    ssh_function "mkdir -p '${SCRIPT_DIR}'"

    # .env（若不存在才建立）
    ssh_function "[ ! -f '${REMOTE_BASE}/.env' ] && printf 'BLUE_VERSION=\nGREEN_VERSION=\n' > '${REMOTE_BASE}/.env' && echo '✅ .env 建立' || echo '⏭  .env 已存在，跳過'"

    # 生成三支 scripts（模板替換 {{PROJECT_NAME}} 與 {{SCRIPT_NAME}}）
    for SCRIPT_FILE in deploy.sh rollback.sh switch_traffic.sh; do
        TPL="${TPL_DIR}/${SCRIPT_FILE}.tpl"
        if [[ ! -f "$TPL" ]]; then
            echo "⚠️  模板不存在，跳過: ${TPL}"
            continue
        fi

        CONTENT=$(sed \
            -e "s|{{PROJECT_NAME}}|${PROJECT}|g" \
            -e "s|{{SCRIPT_NAME}}|${SCRIPT_NAME}|g" \
            "${TPL}")

        REMOTE_SCRIPT="${SCRIPT_DIR}/${SCRIPT_FILE}"
        echo "$CONTENT" | ssh_function "cat > '${REMOTE_SCRIPT}' && chmod +x '${REMOTE_SCRIPT}' && echo '✅ ${SCRIPT_FILE} 寫入完成'"
    done

    echo "✅ [PROD-init] ${PROJECT} 初始化完成: ${REMOTE_BASE}"
fi

# ──────────────────────────────────────────────────────────────
# DEV 初始化邏輯（目前保留，視需求補充）
# ──────────────────────────────────────────────────────────────
if [[ "$INIT_TYPE" == "dev" ]]; then
    echo "⏭  [DEV-init] DEV 初始化邏輯目前無需額外操作（docker-compose 請透過編輯器維護）"
fi