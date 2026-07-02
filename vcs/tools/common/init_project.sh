#!/bin/bash
# =============================================================
#  init_project.sh  [prod|dev]  <projectName>  <scriptName>
#  由 VCS 後台呼叫，在遠端機器初始化新專案的目錄結構
#
#  prod:
#    - /opt/docker_image/${PROJECT}/                  確保存在
#    - /opt/docker_image/${PROJECT}/.env              建立（若不存在）
#    - /opt/docker_image/${PROJECT}/script/           建立
#    - /opt/docker_image/${PROJECT}/script/deploy.sh  從模板生成
#    - /opt/docker_image/${PROJECT}/script/rollback.sh
#    - /opt/docker_image/${PROJECT}/script/switch_traffic.sh
#
#  dev:
#    - /opt/docker_image/docker-compose.yml           追加新 service（若 service 不存在）
# =============================================================
ENV=$1
PROJECT=$2     # DB project name（docker 目錄名）
SCRIPT_NAME=$3 # tools/ 目錄名（可能與 PROJECT 不同）

TOOLS_BASE="/opt/vcs/tools"
TPL_DIR="${TOOLS_BASE}/common/templates"
DOCKER_IMAGE_BASE="/opt/docker_image"

# Validation
[[ "$ENV" != "prod" && "$ENV" != "dev" ]] && { echo "Usage: init_project.sh [prod|dev] <project> <scriptName>"; exit 1; }
[[ -z "$PROJECT" ]]                        && { echo "❌ projectName required"; exit 1; }
[[ -z "$SCRIPT_NAME" ]]                    && SCRIPT_NAME="$PROJECT"

source "${TOOLS_BASE}/utils/sshToolUtil.sh" "$ENV"

# ──────────────────────────────────────────────────────────────
# PROD：建立目錄 + 生成 scripts
# ──────────────────────────────────────────────────────────────
if [[ "$ENV" == "prod" ]]; then
    REMOTE_BASE="${DOCKER_IMAGE_BASE}/${PROJECT}"
    SCRIPT_DIR="${REMOTE_BASE}/script"

    echo "▶ [PROD] 建立目錄: ${REMOTE_BASE}"
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

        # 本機替換 → pipe 到遠端寫入
        CONTENT=$(sed \
            -e "s|{{PROJECT_NAME}}|${PROJECT}|g" \
            -e "s|{{SCRIPT_NAME}}|${SCRIPT_NAME}|g" \
            "${TPL}")

        REMOTE_SCRIPT="${SCRIPT_DIR}/${SCRIPT_FILE}"
        echo "$CONTENT" | ssh_function "cat > '${REMOTE_SCRIPT}' && chmod +x '${REMOTE_SCRIPT}' && echo '✅ ${SCRIPT_FILE} 寫入完成'"
    done

    echo "✅ [PROD] ${PROJECT} 初始化完成: ${REMOTE_BASE}"
fi

# ──────────────────────────────────────────────────────────────
# DEV：追加 service 到共用 docker-compose.yml
# ──────────────────────────────────────────────────────────────
# if [[ "$ENV" == "dev" ]]; then
#     DEV_COMPOSE="/opt/docker_image/docker-compose.yml"

#     # 確認 compose 檔存在
#     EXISTS=$(ssh_function "[ -f '${DEV_COMPOSE}' ] && echo yes || echo no")
#     if [[ "$EXISTS" != "yes" ]]; then
#         echo "❌ DEV docker-compose.yml 不存在: ${DEV_COMPOSE}"
#         exit 1
#     fi

#     # 確認 service 是否已存在
#     ALREADY=$(ssh_function "grep -c '^\s*${PROJECT}-blue:' '${DEV_COMPOSE}' 2>/dev/null || echo 0")
#     if [[ "$ALREADY" -gt 0 ]]; then
#         echo "⏭  [DEV] service '${PROJECT}-blue' 已存在於 docker-compose.yml，跳過"
#         exit 0
#     fi

#     # 讀取 config 取得 DEV 欄位
#     CONFIG_FILE="${TOOLS_BASE}/${SCRIPT_NAME}/config.sh"
#     [[ ! -f "$CONFIG_FILE" ]] && { echo "⚠️  ${CONFIG_FILE} 不存在，無法生成 service 片段，請先填寫配置再重試"; exit 1; }
#     source "$CONFIG_FILE"

#     DEV_IMAGE_REPO="${DEV_IMAGE_REPO:-backend-dev}"
#     DEV_PORT="${DEV_BLUE_CHECK_PORTS[0]:-}"
#     DEV_DEPLOY_BASE="${DEV_DEPLOY_BASE:-/opt/docker_image}"

#     SERVICE_BLOCK=$(cat <<YAML

#   # ── ${PROJECT} (added by VCS init) ──
#   ${PROJECT}-blue:
#     image: \${REGISTRY}/${DEV_IMAGE_REPO}/\${DEV_IMAGE_KEYWORD:-${PROJECT}}:\${DEV_BLUE_VERSION:-latest}
#     container_name: ${PROJECT}-blue
#     restart: unless-stopped
#     ports:
#       - "${DEV_PORT}:${DEV_PORT}"
#     env_file:
#       - .env
#     volumes:
#       - ${DEV_DEPLOY_BASE}/${PROJECT}/blue/webapp:/app/webapp
# YAML
# )

#     echo "$SERVICE_BLOCK" | ssh_function "cat >> '${DEV_COMPOSE}' && echo '✅ [DEV] service ${PROJECT}-blue 已追加至 docker-compose.yml'"
# fi
